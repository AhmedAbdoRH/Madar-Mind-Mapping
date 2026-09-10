import * as fs from "fs";
import * as path from "path";
import { ChecklistItem, MindMap, MindMapPackage, MindNode, NodeTreeNode, MadarBackupFile } from "./types.js";

const DEFAULT_WORKSPACE_DIR = process.env.MADAR_STORAGE_PATH || path.join(process.cwd(), "madar_workspace");

export class MadarStorage {
  private workspaceDir: string;
  private dbFile: string;
  private maps: Map<number, MindMap> = new Map();
  private nodes: Map<string, MindNode> = new Map();
  private nextMapId: number = 1;

  constructor(workspaceDir: string = DEFAULT_WORKSPACE_DIR) {
    this.workspaceDir = workspaceDir;
    this.dbFile = path.join(this.workspaceDir, "madar_database.json");
    this.init();
  }

  private init() {
    if (!fs.existsSync(this.workspaceDir)) {
      fs.mkdirSync(this.workspaceDir, { recursive: true });
    }
    if (fs.existsSync(this.dbFile)) {
      try {
        const raw = fs.readFileSync(this.dbFile, "utf-8");
        const data = JSON.parse(raw);
        if (Array.isArray(data.maps)) {
          for (const m of data.maps) {
            this.maps.set(m.id, m);
            if (m.id >= this.nextMapId) this.nextMapId = m.id + 1;
          }
        }
        if (Array.isArray(data.nodes)) {
          for (const n of data.nodes) {
            this.nodes.set(n.id, n);
          }
        }
      } catch (e) {
        console.error("Failed to load existing database, starting fresh:", e);
      }
    } else {
      // Create initial sample Mind Map
      this.createSampleUniverse();
    }
  }

  private save() {
    try {
      const data = {
        version: 1,
        maps: Array.from(this.maps.values()),
        nodes: Array.from(this.nodes.values()),
        updatedAt: Date.now()
      };
      fs.writeFileSync(this.dbFile, JSON.stringify(data, null, 2), "utf-8");
    } catch (e) {
      console.error("Failed to persist database:", e);
    }
  }

  private createSampleUniverse() {
    const rootNodeId = "root_" + Date.now();
    const map: MindMap = {
      id: 1,
      title: "خريطتي الأولى (مدار)",
      description: "عالم الأفكار والمشاريع في مدار",
      themeColorHex: "#38BDF8",
      rootNodeId: rootNodeId,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    this.maps.set(map.id, map);
    this.nextMapId = 2;

    const rootNode: MindNode = {
      id: rootNodeId,
      mapId: map.id,
      parentId: null,
      syncMasterId: null,
      title: "المركز الرئيسي",
      notes: "النواة المركزية لخريطتك الذهنية",
      checklistJson: JSON.stringify([
        { id: "chk_1", text: "استكشاف الميزات الجديدة", isDone: true, orderIndex: 0 },
        { id: "chk_2", text: "تحديد مستويات التأثير للأهداف", isDone: false, orderIndex: 1 }
      ]),
      linkUrl: null,
      colorHex: "#38BDF8",
      iconName: "AutoAwesome",
      progress: 50,
      impact: 5,
      orderIndex: 0,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    this.nodes.set(rootNode.id, rootNode);

    // Add 2 child nodes
    const child1Id = "node_" + (Date.now() + 1);
    this.nodes.set(child1Id, {
      id: child1Id,
      mapId: map.id,
      parentId: rootNodeId,
      syncMasterId: null,
      title: "الأهداف الاستراتيجية",
      notes: "تخطيط المشاريع والأولويات",
      checklistJson: null,
      linkUrl: null,
      colorHex: "#F59E0B",
      iconName: "Flag",
      progress: 25,
      impact: 4,
      orderIndex: 0,
      createdAt: Date.now(),
      updatedAt: Date.now()
    });

    const child2Id = "node_" + (Date.now() + 2);
    this.nodes.set(child2Id, {
      id: child2Id,
      mapId: map.id,
      parentId: rootNodeId,
      syncMasterId: null,
      title: "المهام والعمليات",
      notes: "قائمة المهام اليومية",
      checklistJson: null,
      linkUrl: null,
      colorHex: "#10B981",
      iconName: "CheckCircle",
      progress: 80,
      impact: 3,
      orderIndex: 1,
      createdAt: Date.now(),
      updatedAt: Date.now()
    });

    this.save();
  }

  // --- CRUD API ---

  public listMaps(): MindMap[] {
    return Array.from(this.maps.values()).sort((a, b) => b.updatedAt - a.updatedAt);
  }

  public getMap(mapId: number): MindMapPackage | null {
    const map = this.maps.get(mapId);
    if (!map) return null;
    const nodes = Array.from(this.nodes.values()).filter(n => n.mapId === mapId);
    return { map, nodes };
  }

  public createMap(params: {
    title: string;
    description?: string;
    themeColorHex?: string;
    rootTitle?: string;
    rootIcon?: string;
  }): MindMapPackage {
    const id = this.nextMapId++;
    const rootNodeId = "node_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7);
    const themeColor = params.themeColorHex || "#38BDF8";

    const map: MindMap = {
      id,
      title: params.title.trim() || "خريطة جديدة",
      description: params.description?.trim() || "",
      themeColorHex: themeColor,
      rootNodeId,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    const rootNode: MindNode = {
      id: rootNodeId,
      mapId: id,
      parentId: null,
      syncMasterId: null,
      title: params.rootTitle?.trim() || params.title.trim() || "المركز الرئيسي",
      notes: "",
      checklistJson: null,
      linkUrl: null,
      colorHex: themeColor,
      iconName: params.rootIcon || "AutoAwesome",
      progress: null,
      impact: null,
      orderIndex: 0,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    this.maps.set(id, map);
    this.nodes.set(rootNodeId, rootNode);
    this.save();

    return { map, nodes: [rootNode] };
  }

  public deleteMap(mapId: number): boolean {
    if (!this.maps.has(mapId)) return false;
    this.maps.delete(mapId);
    for (const [id, node] of this.nodes.entries()) {
      if (node.mapId === mapId) {
        this.nodes.delete(id);
      }
    }
    this.save();
    return true;
  }

  public addNode(params: {
    mapId: number;
    parentId: string;
    title: string;
    notes?: string;
    colorHex?: string;
    iconName?: string;
    linkUrl?: string;
    progress?: number;
    impact?: number; // 1..5
    checklist?: Array<{ text: string; isDone?: boolean }>;
  }): MindNode | null {
    const map = this.maps.get(params.mapId);
    if (!map) return null;
    const parentNode = this.nodes.get(params.parentId);
    if (!parentNode || parentNode.mapId !== params.mapId) return null;

    const nodeId = "node_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7);
    const siblings = Array.from(this.nodes.values()).filter(
      n => n.mapId === params.mapId && n.parentId === params.parentId
    );

    let checklistJson: string | null = null;
    if (params.checklist && params.checklist.length > 0) {
      checklistJson = JSON.stringify(
        params.checklist.map((item, idx) => ({
          id: "chk_" + Date.now() + "_" + idx,
          text: item.text,
          isDone: !!item.isDone,
          orderIndex: idx
        }))
      );
    }

    const node: MindNode = {
      id: nodeId,
      mapId: params.mapId,
      parentId: params.parentId,
      syncMasterId: null,
      title: params.title.trim(),
      notes: params.notes || "",
      checklistJson,
      linkUrl: params.linkUrl || null,
      colorHex: params.colorHex || parentNode.colorHex || map.themeColorHex,
      iconName: params.iconName || "Star",
      progress: params.progress !== undefined ? Math.max(0, Math.min(100, params.progress)) : null,
      impact: params.impact !== undefined ? Math.max(1, Math.min(5, params.impact)) : null,
      orderIndex: siblings.length,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    this.nodes.set(nodeId, node);
    map.updatedAt = Date.now();
    this.save();

    return node;
  }

  public updateNode(nodeId: string, updates: Partial<{
    title: string;
    notes: string;
    colorHex: string;
    iconName: string;
    linkUrl: string;
    progress: number | null;
    impact: number | null; // 1..5
    orderIndex: number;
  }>): MindNode | null {
    const node = this.nodes.get(nodeId);
    if (!node) return null;

    if (updates.title !== undefined) node.title = updates.title.trim();
    if (updates.notes !== undefined) node.notes = updates.notes;
    if (updates.colorHex !== undefined) node.colorHex = updates.colorHex;
    if (updates.iconName !== undefined) node.iconName = updates.iconName;
    if (updates.linkUrl !== undefined) node.linkUrl = updates.linkUrl;
    if (updates.progress !== undefined) {
      node.progress = updates.progress === null ? null : Math.max(0, Math.min(100, updates.progress));
    }
    if (updates.impact !== undefined) {
      node.impact = updates.impact === null ? null : Math.max(1, Math.min(5, updates.impact));
    }
    if (updates.orderIndex !== undefined) node.orderIndex = updates.orderIndex;

    node.updatedAt = Date.now();

    const map = this.maps.get(node.mapId);
    if (map) map.updatedAt = Date.now();

    this.save();
    return node;
  }

  public deleteNode(nodeId: string): boolean {
    const node = this.nodes.get(nodeId);
    if (!node) return false;
    const map = this.maps.get(node.mapId);
    if (map && map.rootNodeId === nodeId) {
      // Cannot delete root node directly without deleting map
      return false;
    }

    // Recursively collect all descendant IDs
    const toDelete = new Set<string>();
    const queue = [nodeId];
    while (queue.length > 0) {
      const curr = queue.shift()!;
      toDelete.add(curr);
      for (const n of this.nodes.values()) {
        if (n.parentId === curr && !toDelete.has(n.id)) {
          queue.push(n.id);
        }
      }
    }

    for (const id of toDelete) {
      this.nodes.delete(id);
    }

    if (map) map.updatedAt = Date.now();
    this.save();
    return true;
  }

  public addChecklistItem(nodeId: string, text: string): ChecklistItem[] | null {
    const node = this.nodes.get(nodeId);
    if (!node) return null;

    let items: ChecklistItem[] = [];
    if (node.checklistJson) {
      try {
        items = JSON.parse(node.checklistJson);
      } catch {}
    }

    const newItem: ChecklistItem = {
      id: "chk_" + Date.now() + "_" + Math.random().toString(36).substring(2, 6),
      text: text.trim(),
      isDone: false,
      orderIndex: items.length
    };
    items.push(newItem);

    node.checklistJson = JSON.stringify(items);
    node.updatedAt = Date.now();
    this.save();
    return items;
  }

  public toggleChecklistItem(nodeId: string, itemId: string, isDone?: boolean): ChecklistItem[] | null {
    const node = this.nodes.get(nodeId);
    if (!node || !node.checklistJson) return null;

    let items: ChecklistItem[] = [];
    try {
      items = JSON.parse(node.checklistJson);
    } catch {
      return null;
    }

    const target = items.find(i => i.id === itemId);
    if (!target) return null;

    target.isDone = isDone !== undefined ? isDone : !target.isDone;
    node.checklistJson = JSON.stringify(items);
    node.updatedAt = Date.now();
    this.save();
    return items;
  }

  // --- Tree & Visual Hierarchy ---

  public buildTree(mapId: number): NodeTreeNode | null {
    const pkg = this.getMap(mapId);
    if (!pkg) return null;

    const root = pkg.nodes.find(n => n.id === pkg.map.rootNodeId);
    if (!root) return null;

    const parseChecklist = (json?: string | null): ChecklistItem[] => {
      if (!json) return [];
      try { return JSON.parse(json); } catch { return []; }
    };

    const buildSubtree = (node: MindNode): NodeTreeNode => {
      const children = pkg.nodes
        .filter(n => n.parentId === node.id)
        .sort((a, b) => a.orderIndex - b.orderIndex)
        .map(buildSubtree);

      return {
        ...node,
        checklist: parseChecklist(node.checklistJson),
        children
      };
    };

    return buildSubtree(root);
  }

  // --- Import / Export (.madar format) ---

  public exportSingleMapBackup(mapId: number): MadarBackupFile | null {
    const pkg = this.getMap(mapId);
    if (!pkg) return null;

    return {
      format: "madar_backup",
      version: 1,
      backupType: "single_map",
      exportedAt: Date.now(),
      appVersion: "1.0",
      map: {
        id: pkg.map.id,
        title: pkg.map.title,
        description: pkg.map.description,
        themeColorHex: pkg.map.themeColorHex,
        rootNodeId: pkg.map.rootNodeId,
        createdAt: pkg.map.createdAt,
        updatedAt: pkg.map.updatedAt
      },
      nodes: pkg.nodes.map(n => ({
        id: n.id,
        parentId: n.parentId || null,
        syncMasterId: n.syncMasterId || null,
        title: n.title,
        notes: n.notes || "",
        checklistJson: n.checklistJson || null,
        linkUrl: n.linkUrl || null,
        colorHex: n.colorHex,
        iconName: n.iconName,
        progress: n.progress,
        impact: n.impact,
        orderIndex: n.orderIndex,
        createdAt: n.createdAt,
        updatedAt: n.updatedAt
      })),
      images: {}
    };
  }

  public exportUniverseBackup(): MadarBackupFile {
    const maps = this.listMaps();
    const mapPackages = maps.map(m => {
      const nodes = Array.from(this.nodes.values()).filter(n => n.mapId === m.id);
      return {
        map: {
          id: m.id,
          title: m.title,
          description: m.description,
          themeColorHex: m.themeColorHex,
          rootNodeId: m.rootNodeId,
          createdAt: m.createdAt,
          updatedAt: m.updatedAt
        },
        nodes: nodes.map(n => ({
          id: n.id,
          parentId: n.parentId || null,
          syncMasterId: n.syncMasterId || null,
          title: n.title,
          notes: n.notes || "",
          checklistJson: n.checklistJson || null,
          linkUrl: n.linkUrl || null,
          colorHex: n.colorHex,
          iconName: n.iconName,
          progress: n.progress,
          impact: n.impact,
          orderIndex: n.orderIndex,
          createdAt: n.createdAt,
          updatedAt: n.updatedAt
        }))
      };
    });

    return {
      format: "madar_backup",
      version: 1,
      backupType: "full_backup",
      exportedAt: Date.now(),
      appVersion: "1.0",
      maps: mapPackages,
      images: {}
    };
  }

  public importMadarBackup(backup: MadarBackupFile): { importedMapsCount: number; importedNodesCount: number } {
    let importedMapsCount = 0;
    let importedNodesCount = 0;

    if (backup.backupType === "single_map" && backup.map && Array.isArray(backup.nodes)) {
      const mapId = this.nextMapId++;
      const idMap = new Map<string, string>(); // oldId -> newId

      // Pre-generate new IDs
      for (const rawNode of backup.nodes) {
        idMap.set(rawNode.id, "node_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7));
      }

      const newRootId = idMap.get(backup.map.rootNodeId) || backup.nodes[0]?.id || "root_" + Date.now();
      const newMap: MindMap = {
        id: mapId,
        title: backup.map.title || "خريطة مستوردة",
        description: backup.map.description || "",
        themeColorHex: backup.map.themeColorHex || "#38BDF8",
        rootNodeId: newRootId,
        createdAt: backup.map.createdAt || Date.now(),
        updatedAt: Date.now()
      };
      this.maps.set(mapId, newMap);

      for (const rawNode of backup.nodes) {
        const newId = idMap.get(rawNode.id)!;
        const newParentId = rawNode.parentId ? (idMap.get(rawNode.parentId) || null) : null;
        const node: MindNode = {
          id: newId,
          mapId: mapId,
          parentId: newParentId,
          syncMasterId: null,
          title: rawNode.title || "",
          notes: rawNode.notes || "",
          checklistJson: rawNode.checklistJson || null,
          linkUrl: rawNode.linkUrl || null,
          colorHex: rawNode.colorHex || newMap.themeColorHex,
          iconName: rawNode.iconName || "Star",
          progress: rawNode.progress !== undefined ? rawNode.progress : null,
          impact: rawNode.impact !== undefined ? rawNode.impact : null,
          orderIndex: rawNode.orderIndex || 0,
          createdAt: rawNode.createdAt || Date.now(),
          updatedAt: Date.now()
        };
        this.nodes.set(newId, node);
        importedNodesCount++;
      }
      importedMapsCount++;
    } else if (backup.maps && Array.isArray(backup.maps)) {
      for (const pkg of backup.maps) {
        const subBackup: MadarBackupFile = {
          format: "madar_backup",
          version: 1,
          backupType: "single_map",
          exportedAt: Date.now(),
          appVersion: "1.0",
          map: pkg.map,
          nodes: pkg.nodes
        };
        const res = this.importMadarBackup(subBackup);
        importedMapsCount += res.importedMapsCount;
        importedNodesCount += res.importedNodesCount;
      }
    }

    this.save();
    return { importedMapsCount, importedNodesCount };
  }
}
