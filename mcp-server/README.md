# 🪐 Madar (مدار) MCP Server

خادم **Model Context Protocol (MCP)** لتطبيق الخرائط الذهنية **مدار (Madar)**، متوافق بنسبة 100% مع **Claude Desktop**، **Claude Code**، وجميع عملاء بروتوكول MCP.

يتيح لـ **Claude** إنشاء وقراءة وتعديل وتنظيم الخرائط الذهنية والعقد والمهام وتحديد مستويات التأثير (Impact Power 1..5) وتصدير ملفات `.madar` المتوافقة مع التطبيق ونظام المزامنة التلقائية.

---

## 🚀 التثبيت والتشغيل السريع (Quick Setup)

### 1. المتطلبات (Prerequisites)
- تثبيت **Node.js** (إصدار 18 أو أحدث).
- تطبيق **Claude Desktop**.

### 2. التثبيت والبناء (Install & Build)

داخل مجلد `mcp-server`:

```bash
cd mcp-server
npm install
npm run build
```

---

## ⚙️ إعداد Claude Desktop (`claude_desktop_config.json`)

افتح ملف إعدادات Claude Desktop:

- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

أضف خادم `madar` تحت قسم `mcpServers`:

```json
{
  "mcpServers": {
    "madar": {
      "command": "node",
      "args": [
        "/ABSOLUTE/PATH/TO/mcp-server/dist/index.js"
      ],
      "env": {
        "MADAR_STORAGE_PATH": "/ABSOLUTE/PATH/TO/madar_workspace"
      }
    }
  }
}
```

> 💡 **ملاحظة**: استبدل `/ABSOLUTE/PATH/TO/` بالمسار الكامل للمجلد على جهازك.

ثم أعد تشغيل تطبيق **Claude Desktop**. ستظهر أيقونة الأدوات 🔨 وتتعرف على أدوات **Madar** فوراً!

---

## 🛠️ الأدوات المتاحة لـ Claude (Tools)

| الأداة | الوصف |
| :--- | :--- |
| `madar_list_maps` | استعراض كافة الخرائط الذهنية والعوالم مع عدد العقد وتاريخ التعديل |
| `madar_get_map` | قراءة الخريطة بالكامل كشجرة هرمية تفاعلية مع المهام ومستويات التأثير |
| `madar_create_map` | إنشاء خريطة ذهنية جديدة (عالم) باسم ولون ونواة رئيسية |
| `madar_add_node` | إضافة عقدة مدارية جديدة متفرعة مع المهام وقوة التأثير (⚡ 1..5) |
| `madar_update_node` | تعديل العنوان، الملاحظات، اللون، التقدم، أو قوة التأثير |
| `madar_delete_node` | حذف عقدة وكافة الفروع التابعة لها |
| `madar_add_checklist_item` | إضافة مهمة جديدة داخل العقدة |
| `madar_toggle_checklist_item` | تحديد إنجاز مهمة معينة |
| `madar_build_mindmap_from_outline` | بناء خريطة ذهنية كاملة متعددة الفروع والمستويات في خطوة واحدة |
| `madar_export_madar_file` | تصدير الخريطة بصيغة `.madar` المتوافقة مباشرة مع تطبيق أندرويد وGoogle Drive |
| `madar_import_madar_file` | استيراد ملفات `.madar` أو النسخ الاحتياطية إلى بيئة العمل |

---

## 💬 أمثلة للأوامر التي يمكنك طلبها من Claude

- *"أنشئ خريطة ذهنية جديدة لخطة إطلاق تطبيق هاتف ذكي، مع تقسيمها إلى 4 محاور وإعطاء الأولويات أعلى قوة تأثير."*
- *"اعرض لي شجرة خريطة العمل وحدد لي العقد ذات التأثير الأقوى (⚡ 5)."*
- *"أضف مهمة 'مراجعة التصميم' داخل عقدة 'واجهة المستخدم'."*
- *"قم بتصدير الخريطة الحالية كملف `.madar` لكي أفتحها على هاتفي في تطبيق مدار."*

---

## 📦 التوافق مع تطبيق أندرويد (Android App Integration)

الملفات المصدرة عبر `madar_export_madar_file` متوافقة بنسبة 100% مع:
1. زر **استيراد من ملف (.madar)** في تطبيق مدار.
2. المزامنة التلقائية والنسخ الاحتياطي السحابي عبر **Google Drive**.
3. دعم كامل للملاحظات، المهام، الألوان، وقوة التأثير (`impact: 1..5`).
