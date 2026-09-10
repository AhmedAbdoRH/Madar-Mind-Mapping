export interface ChecklistItem {
  id: string;
  text: string;
  isDone: boolean;
  orderIndex: number;
}

export interface MindNode {
  id: string;
  mapId: number;
  parentId?: string | null;
  syncMasterId?: string | null;
  title: string;
  notes?: string | null;
  checklistJson?: string | null;
  linkUrl?: string | null;
  colorHex: string;
  iconName?: string | null;
  imageUri?: string | null;
  progress?: number | null; // 0..100
  impact?: number | null;   // 1..5 (قوة التأثير)
  orderIndex: number;
  createdAt: number;
  updatedAt: number;
}

export interface MindMap {
  id: number;
  title: string;
  description: string;
  themeColorHex: string;
  rootNodeId: string;
  createdAt: number;
  updatedAt: number;
}

export interface NodeTreeNode extends MindNode {
  checklist: ChecklistItem[];
  children: NodeTreeNode[];
}

export interface MindMapPackage {
  map: MindMap;
  nodes: MindNode[];
}

export interface MadarBackupFile {
  format: "madar_backup";
  version: number;
  backupType: "single_map" | "full_backup";
  exportedAt: number;
  appVersion: string;
  map?: any;
  nodes?: any[];
  maps?: Array<{
    map: any;
    nodes: any[];
  }>;
  images?: Record<string, string>;
}
