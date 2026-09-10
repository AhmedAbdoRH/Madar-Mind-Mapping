#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
  ListPromptsRequestSchema,
  GetPromptRequestSchema,
  ErrorCode,
  McpError
} from "@modelcontextprotocol/sdk/types.js";
import { MadarStorage } from "./storage.js";
import * as fs from "fs";
import * as path from "path";

// Initialize Storage
const storage = new MadarStorage();

// Create MCP Server
const server = new Server(
  {
    name: "madar-mcp-server",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
      resources: {},
      prompts: {},
    },
  }
);

// ==========================================
// TOOLS DEFINITION
// ==========================================

server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "madar_list_maps",
        description: "List all mind maps / universes in the Madar workspace with high-level statistics (ID, title, description, color, node count, last updated).",
        inputSchema: {
          type: "object",
          properties: {},
        },
      },
      {
        name: "madar_get_map",
        description: "Retrieve full details of a specific mind map, including all nodes, checklists, notes, progress, and impact levels. Can return as a visual hierarchy tree or flat node list.",
        inputSchema: {
          type: "object",
          properties: {
            mapId: {
              type: "number",
              description: "The unique ID of the mind map to retrieve",
            },
            asTree: {
              type: "boolean",
              description: "Whether to return the nodes formatted as a nested hierarchical tree (default: true)",
            },
          },
          required: ["mapId"],
        },
      },
      {
        name: "madar_create_map",
        description: "Create a new mind map (universe) with a title, description, theme color, and root node.",
        inputSchema: {
          type: "object",
          properties: {
            title: {
              type: "string",
              description: "Title of the mind map / universe",
            },
            description: {
              type: "string",
              description: "Optional summary description",
            },
            themeColorHex: {
              type: "string",
              description: "Hex color for the map theme (e.g. #38BDF8, #818CF8, #F59E0B, #10B981, #EC4899)",
            },
            rootTitle: {
              type: "string",
              description: "Title of the root center node (defaults to map title)",
            },
            rootIcon: {
              type: "string",
              description: "Icon name for root node (e.g. AutoAwesome, Explore, Flag, Lightbulb, Rocket)",
            },
          },
          required: ["title"],
        },
      },
      {
        name: "madar_add_node",
        description: "Add a new satellite / child node under an existing parent node in a mind map.",
        inputSchema: {
          type: "object",
          properties: {
            mapId: {
              type: "number",
              description: "The ID of the mind map containing the parent node",
            },
            parentId: {
              type: "string",
              description: "The ID of the parent node to attach this new node to",
            },
            title: {
              type: "string",
              description: "The title / name of the new node",
            },
            notes: {
              type: "string",
              description: "Optional markdown / plain text notes inside the node",
            },
            colorHex: {
              type: "string",
              description: "Optional hex color for this node (inherits from parent if omitted)",
            },
            iconName: {
              type: "string",
              description: "Optional icon name (e.g. Star, Flag, CheckCircle, Lightbulb, TrendingUp, Folder, Code)",
            },
            linkUrl: {
              type: "string",
              description: "Optional web URL attached to this node",
            },
            progress: {
              type: "number",
              description: "Optional progress percentage from 0 to 100",
            },
            impact: {
              type: "number",
              description: "Optional Impact Power level (قوة التأثير) from 1 (lowest) to 5 (highest/critical)",
            },
            checklist: {
              type: "array",
              description: "Optional checklist items for this node",
              items: {
                type: "object",
                properties: {
                  text: { type: "string" },
                  isDone: { type: "boolean" },
                },
                required: ["text"],
              },
            },
          },
          required: ["mapId", "parentId", "title"],
        },
      },
      {
        name: "madar_update_node",
        description: "Update the title, notes, color, progress, impact rating (1..5), or URL of an existing node.",
        inputSchema: {
          type: "object",
          properties: {
            nodeId: {
              type: "string",
              description: "The unique ID of the node to update",
            },
            title: {
              type: "string",
              description: "New title for the node",
            },
            notes: {
              type: "string",
              description: "New notes content",
            },
            colorHex: {
              type: "string",
              description: "New hex color for the node",
            },
            iconName: {
              type: "string",
              description: "New icon name",
            },
            linkUrl: {
              type: "string",
              description: "New link URL",
            },
            progress: {
              type: "number",
              description: "New progress percentage (0..100) or null to clear",
            },
            impact: {
              type: "number",
              description: "New Impact Power rating (1..5) or null to clear",
            },
          },
          required: ["nodeId"],
        },
      },
      {
        name: "madar_delete_node",
        description: "Delete a node and all of its recursive child sub-nodes from a mind map.",
        inputSchema: {
          type: "object",
          properties: {
            nodeId: {
              type: "string",
              description: "The ID of the node to remove",
            },
          },
          required: ["nodeId"],
        },
      },
      {
        name: "madar_add_checklist_item",
        description: "Add a task / checklist item to a specific node.",
        inputSchema: {
          type: "object",
          properties: {
            nodeId: {
              type: "string",
              description: "The ID of the node",
            },
            text: {
              type: "string",
              description: "The task text",
            },
          },
          required: ["nodeId", "text"],
        },
      },
      {
        name: "madar_toggle_checklist_item",
        description: "Check, uncheck, or toggle a checklist item on a node.",
        inputSchema: {
          type: "object",
          properties: {
            nodeId: {
              type: "string",
              description: "The ID of the node containing the checklist",
            },
            itemId: {
              type: "string",
              description: "The ID of the checklist item (e.g. chk_...)",
            },
            isDone: {
              type: "boolean",
              description: "Optional explicit boolean state (if omitted, toggles current value)",
            },
          },
          required: ["nodeId", "itemId"],
        },
      },
      {
        name: "madar_build_mindmap_from_outline",
        description: "Build an entire multi-level mind map in one action from a structured hierarchical outline or text brainstorm.",
        inputSchema: {
          type: "object",
          properties: {
            title: {
              type: "string",
              description: "Title of the new mind map",
            },
            themeColorHex: {
              type: "string",
              description: "Theme color for the map",
            },
            branches: {
              type: "array",
              description: "List of main branch nodes with their sub-branches, notes, checklists, and impact ratings",
              items: {
                type: "object",
                properties: {
                  title: { type: "string" },
                  notes: { type: "string" },
                  colorHex: { type: "string" },
                  iconName: { type: "string" },
                  impact: { type: "number", description: "Impact 1..5" },
                  progress: { type: "number", description: "Progress 0..100" },
                  checklist: {
                    type: "array",
                    items: {
                      type: "object",
                      properties: {
                        text: { type: "string" },
                        isDone: { type: "boolean" },
                      },
                      required: ["text"],
                    },
                  },
                  subBranches: {
                    type: "array",
                    items: {
                      type: "object",
                      properties: {
                        title: { type: "string" },
                        notes: { type: "string" },
                        colorHex: { type: "string" },
                        impact: { type: "number" },
                        checklist: {
                          type: "array",
                          items: { type: "string" },
                        },
                      },
                      required: ["title"],
                    },
                  },
                },
                required: ["title"],
              },
            },
          },
          required: ["title", "branches"],
        },
      },
      {
        name: "madar_export_madar_file",
        description: "Export a mind map or the entire workspace as a native .madar backup JSON file compatible with the Madar Android app and Google Drive AutoSync.",
        inputSchema: {
          type: "object",
          properties: {
            mapId: {
              type: "number",
              description: "Optional map ID to export a single map. If omitted, exports full universe backup.",
            },
            outputPath: {
              type: "string",
              description: "Optional file path to save the .madar file (e.g. ./my_map.madar)",
            },
          },
        },
      },
      {
        name: "madar_import_madar_file",
        description: "Import a .madar or backup JSON file into the local workspace.",
        inputSchema: {
          type: "object",
          properties: {
            filePathOrJson: {
              type: "string",
              description: "Absolute/relative file path to the .madar file or raw JSON backup string",
            },
          },
          required: ["filePathOrJson"],
        },
      },
    ],
  };
});

// ==========================================
// TOOLS HANDLER
// ==========================================

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "madar_list_maps": {
        const maps = storage.listMaps();
        const results = maps.map((m) => {
          const pkg = storage.getMap(m.id);
          return {
            id: m.id,
            title: m.title,
            description: m.description,
            themeColorHex: m.themeColorHex,
            nodeCount: pkg?.nodes.length || 0,
            updatedAt: new Date(m.updatedAt).toISOString(),
          };
        });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(results, null, 2),
            },
          ],
        };
      }

      case "madar_get_map": {
        const mapId = Number(args?.mapId);
        const asTree = args?.asTree !== false;

        if (asTree) {
          const tree = storage.buildTree(mapId);
          if (!tree) {
            throw new McpError(ErrorCode.InvalidRequest, `Mind map with ID ${mapId} not found`);
          }
          const pkg = storage.getMap(mapId);
          return {
            content: [
              {
                type: "text",
                text: JSON.stringify(
                  {
                    map: pkg?.map,
                    tree: tree,
                  },
                  null,
                  2
                ),
              },
            ],
          };
        } else {
          const pkg = storage.getMap(mapId);
          if (!pkg) {
            throw new McpError(ErrorCode.InvalidRequest, `Mind map with ID ${mapId} not found`);
          }
          return {
            content: [
              {
                type: "text",
                text: JSON.stringify(pkg, null, 2),
              },
            ],
          };
        }
      }

      case "madar_create_map": {
        const result = storage.createMap({
          title: String(args?.title || ""),
          description: args?.description ? String(args.description) : undefined,
          themeColorHex: args?.themeColorHex ? String(args.themeColorHex) : undefined,
          rootTitle: args?.rootTitle ? String(args.rootTitle) : undefined,
          rootIcon: args?.rootIcon ? String(args.rootIcon) : undefined,
        });

        return {
          content: [
            {
              type: "text",
              text: `✅ Successfully created Mind Map: "${result.map.title}" (ID: ${result.map.id})\nRoot Node ID: ${result.map.rootNodeId}`,
            },
          ],
        };
      }

      case "madar_add_node": {
        const node = storage.addNode({
          mapId: Number(args?.mapId),
          parentId: String(args?.parentId),
          title: String(args?.title),
          notes: args?.notes ? String(args.notes) : undefined,
          colorHex: args?.colorHex ? String(args.colorHex) : undefined,
          iconName: args?.iconName ? String(args.iconName) : undefined,
          linkUrl: args?.linkUrl ? String(args.linkUrl) : undefined,
          progress: args?.progress !== undefined ? Number(args.progress) : undefined,
          impact: args?.impact !== undefined ? Number(args.impact) : undefined,
          checklist: Array.isArray(args?.checklist) ? (args.checklist as any) : undefined,
        });

        if (!node) {
          throw new McpError(ErrorCode.InvalidRequest, "Failed to add node. Verify mapId and parentId exist.");
        }

        return {
          content: [
            {
              type: "text",
              text: `✅ Added node "${node.title}" (ID: ${node.id}) under parent (${node.parentId})${node.impact ? ` [⚡ Impact: ${node.impact}/5]` : ""}`,
            },
          ],
        };
      }

      case "madar_update_node": {
        const nodeId = String(args?.nodeId);
        const node = storage.updateNode(nodeId, {
          title: args?.title ? String(args.title) : undefined,
          notes: args?.notes ? String(args.notes) : undefined,
          colorHex: args?.colorHex ? String(args.colorHex) : undefined,
          iconName: args?.iconName ? String(args.iconName) : undefined,
          linkUrl: args?.linkUrl ? String(args.linkUrl) : undefined,
          progress: args?.progress !== undefined ? (args.progress === null ? null : Number(args.progress)) : undefined,
          impact: args?.impact !== undefined ? (args.impact === null ? null : Number(args.impact)) : undefined,
        });

        if (!node) {
          throw new McpError(ErrorCode.InvalidRequest, `Node with ID ${nodeId} not found`);
        }

        return {
          content: [
            {
              type: "text",
              text: `✅ Updated node "${node.title}" (ID: ${node.id})`,
            },
          ],
        };
      }

      case "madar_delete_node": {
        const nodeId = String(args?.nodeId);
        const success = storage.deleteNode(nodeId);
        if (!success) {
          throw new McpError(ErrorCode.InvalidRequest, `Failed to delete node ${nodeId}. Cannot delete root node or nonexistent node.`);
        }
        return {
          content: [
            {
              type: "text",
              text: `🗑️ Successfully deleted node ${nodeId} and all child branches.`,
            },
          ],
        };
      }

      case "madar_add_checklist_item": {
        const nodeId = String(args?.nodeId);
        const text = String(args?.text);
        const list = storage.addChecklistItem(nodeId, text);
        if (!list) {
          throw new McpError(ErrorCode.InvalidRequest, `Node ${nodeId} not found`);
        }
        return {
          content: [
            {
              type: "text",
              text: `✅ Task added to node ${nodeId}. Total items: ${list.length}`,
            },
          ],
        };
      }

      case "madar_toggle_checklist_item": {
        const nodeId = String(args?.nodeId);
        const itemId = String(args?.itemId);
        const isDone = args?.isDone !== undefined ? Boolean(args.isDone) : undefined;
        const list = storage.toggleChecklistItem(nodeId, itemId, isDone);
        if (!list) {
          throw new McpError(ErrorCode.InvalidRequest, `Node or checklist item not found`);
        }
        return {
          content: [
            {
              type: "text",
              text: `✅ Updated task ${itemId} on node ${nodeId}`,
            },
          ],
        };
      }

      case "madar_build_mindmap_from_outline": {
        const title = String(args?.title || "خريطة تفاعلية");
        const themeColorHex = args?.themeColorHex ? String(args.themeColorHex) : "#38BDF8";
        const branches = Array.isArray(args?.branches) ? args.branches : [];

        const created = storage.createMap({
          title,
          themeColorHex,
          rootTitle: title,
        });

        const rootId = created.map.rootNodeId;
        let totalNodesCreated = 1;

        for (const branch of branches) {
          const mainNode = storage.addNode({
            mapId: created.map.id,
            parentId: rootId,
            title: branch.title,
            notes: branch.notes,
            colorHex: branch.colorHex,
            iconName: branch.iconName,
            impact: branch.impact,
            progress: branch.progress,
            checklist: branch.checklist,
          });

          if (mainNode) {
            totalNodesCreated++;
            if (Array.isArray(branch.subBranches)) {
              for (const sub of branch.subBranches) {
                const subChecklist = Array.isArray(sub.checklist)
                  ? sub.checklist.map((t: string) => ({ text: t, isDone: false }))
                  : undefined;

                const subNode = storage.addNode({
                  mapId: created.map.id,
                  parentId: mainNode.id,
                  title: sub.title,
                  notes: sub.notes,
                  colorHex: sub.colorHex,
                  impact: sub.impact,
                  checklist: subChecklist,
                });
                if (subNode) totalNodesCreated++;
              }
            }
          }
        }

        return {
          content: [
            {
              type: "text",
              text: `🎉 Built complete mind map "${created.map.title}" (ID: ${created.map.id}) with ${totalNodesCreated} nodes and structured branches!`,
            },
          ],
        };
      }

      case "madar_export_madar_file": {
        const mapId = args?.mapId ? Number(args.mapId) : undefined;
        const backup = mapId ? storage.exportSingleMapBackup(mapId) : storage.exportUniverseBackup();

        if (!backup) {
          throw new McpError(ErrorCode.InvalidRequest, `Could not export backup for map ID ${mapId}`);
        }

        const jsonString = JSON.stringify(backup, null, 2);
        if (args?.outputPath) {
          const outPath = String(args.outputPath);
          fs.writeFileSync(outPath, jsonString, "utf-8");
          return {
            content: [
              {
                type: "text",
                text: `💾 Exported Madar backup successfully to file: ${outPath} (${Buffer.byteLength(jsonString)} bytes)`,
              },
            ],
          };
        }

        return {
          content: [
            {
              type: "text",
              text: jsonString,
            },
          ],
        };
      }

      case "madar_import_madar_file": {
        const input = String(args?.filePathOrJson || "");
        let jsonStr = input;
        if (fs.existsSync(input)) {
          jsonStr = fs.readFileSync(input, "utf-8");
        }

        const parsed = JSON.parse(jsonStr);
        const res = storage.importMadarBackup(parsed);

        return {
          content: [
            {
              type: "text",
              text: `📥 Imported ${res.importedMapsCount} mind map(s) with ${res.importedNodesCount} nodes into Madar workspace.`,
            },
          ],
        };
      }

      default:
        throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${name}`);
    }
  } catch (error: any) {
    return {
      content: [
        {
          type: "text",
          text: `❌ Error executing ${name}: ${error.message || String(error)}`,
        },
      ],
      isError: true,
    };
  }
});

// ==========================================
// RESOURCES DEFINITION
// ==========================================

server.setRequestHandler(ListResourcesRequestSchema, async () => {
  const maps = storage.listMaps();
  return {
    resources: [
      {
        uri: "madar://maps",
        name: "All Madar Mind Maps",
        description: "List of all mind maps in the workspace",
        mimeType: "application/json",
      },
      ...maps.map((m) => ({
        uri: `madar://maps/${m.id}`,
        name: `Mind Map: ${m.title}`,
        description: m.description || `Mind map hierarchy with root node ${m.rootNodeId}`,
        mimeType: "application/json",
      })),
    ],
  };
});

server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const uri = request.params.uri;

  if (uri === "madar://maps") {
    const maps = storage.listMaps();
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify(maps, null, 2),
        },
      ],
    };
  }

  const match = uri.match(/^madar:\/\/maps\/(\d+)$/);
  if (match) {
    const mapId = Number(match[1]);
    const tree = storage.buildTree(mapId);
    if (!tree) {
      throw new McpError(ErrorCode.InvalidRequest, `Resource not found: ${uri}`);
    }
    const pkg = storage.getMap(mapId);
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify({ map: pkg?.map, hierarchy: tree }, null, 2),
        },
      ],
    };
  }

  throw new McpError(ErrorCode.InvalidRequest, `Invalid resource URI: ${uri}`);
});

// ==========================================
// PROMPTS TEMPLATES
// ==========================================

server.setRequestHandler(ListPromptsRequestSchema, async () => {
  return {
    prompts: [
      {
        name: "brainstorm_mindmap",
        description: "Brainstorm and build a rich, multi-layered mind map in Madar for any subject or project.",
        arguments: [
          {
            name: "topic",
            description: "The topic, project, or domain to brainstorm into a mind map",
            required: true,
          },
          {
            name: "depth",
            description: "Depth level: shallow (1-2 levels) or deep (3+ levels with tasks and impact power)",
            required: false,
          },
        ],
      },
      {
        name: "prioritize_by_impact",
        description: "Analyze an existing mind map's nodes, tasks, and impact ratings (1..5) to provide strategic recommendations.",
        arguments: [
          {
            name: "mapId",
            description: "ID of the mind map to analyze",
            required: true,
          },
        ],
      },
    ],
  };
});

server.setRequestHandler(GetPromptRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  if (name === "brainstorm_mindmap") {
    const topic = args?.topic || "مشروع جديد";
    return {
      description: `Brainstorm a mind map for ${topic}`,
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text: `You are an expert brainstorming and visual thinking assistant for the Madar (مدار) mind mapping application.\n\nPlease create a comprehensive, well-structured mind map for the topic: "${topic}".\n\nGuidelines:\n1. Choose an appropriate color palette for the main branches.\n2. Add clear concise titles and informative notes for key nodes.\n3. Assign Impact Power (1 to 5) for important objectives and tasks.\n4. Call the \`madar_build_mindmap_from_outline\` tool to create it directly in Madar!`,
          },
        },
      ],
    };
  }

  if (name === "prioritize_by_impact") {
    const mapId = args?.mapId || "1";
    return {
      description: `Prioritize mind map #${mapId} by Impact Power`,
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text: `Please fetch mind map ID ${mapId} using \`madar_get_map\`, analyze its node structure, checklists, and impact ratings (1 to 5), and provide a prioritisation matrix highlighting high-impact quick wins and strategic goals.`,
          },
        },
      ],
    };
  }

  throw new McpError(ErrorCode.InvalidRequest, `Prompt not found: ${name}`);
});

// ==========================================
// RUN SERVER VIA STDIO TRANSPORT
// ==========================================

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("🚀 Madar MCP Server running on stdio transport");
}

main().catch((err) => {
  console.error("Fatal error starting Madar MCP server:", err);
  process.exit(1);
});
