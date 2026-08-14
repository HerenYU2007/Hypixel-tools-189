import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = "C:\\Users\\97105\\Documents\\1.8.9";
const logPath = path.join(root, "outputs", "fireballpredictor-agent-load.log");
const outputDir = path.join(root, "outputs", "protection1");
const desktopPath = "C:\\Users\\97105\\Desktop\\protection1-combo-damage-table.xlsx";
const rawJsonPath = path.join(outputDir, "protection1-raw.json");
const outputPath = path.join(outputDir, "protection1-combo-damage-table.xlsx");
const previewPath = path.join(outputDir, "protection1-combo-damage-table-preview.png");
const cutoff = "2026-08-13 17:06:40";

const armorNames = {
  leather: "\u76ae\u9769\u5957",
  iron: "\u94c1\u5957",
  diamond: "\u94bb\u5957",
  unknown: "\u672a\u77e5",
};

const swordNames = {
  wood_sword: "\u6728\u5251",
  gold_sword: "\u91d1\u5251",
  stone_sword: "\u77f3\u5251",
  iron_sword: "\u94c1\u5251",
  diamond_sword: "\u94bb\u77f3\u5251",
  none: "\u65e0",
};

const protectionZeroBaseline = new Map(Object.entries({
  "leather|wood_sword|0|false": [4, 7, 8, 10, 11],
  "leather|stone_sword|0|false": [5, 8, 9, 13],
  "leather|iron_sword|0|false": [5, 6, 10],
  "leather|diamond_sword|0|false": [5, 6, 11, 12],
  "iron|wood_sword|0|false": [1, 3, 4, 6],
  "iron|stone_sword|0|false": [4, 6, 7, 10],
  "iron|iron_sword|0|false": [3, 4, 7, 8],
  "iron|diamond_sword|0|false": [3, 5, 9, 13],
  "diamond|wood_sword|0|false": [2, 3, 4, 5, 6],
  "diamond|stone_sword|0|false": [2, 5, 6],
  "diamond|iron_sword|0|false": [3, 4, 6, 7, 9, 10],
  "diamond|diamond_sword|0|false": [4, 7, 8],
}));

function baselineStatus(row) {
  const sword = row.sword === "gold_sword" ? "wood_sword" : row.sword;
  const values = protectionZeroBaseline.get(`${row.armor}|${sword}|${row.sharp}|${row.crit}`);
  if (!values) {
    return "\u65e0\u57fa\u51c6";
  }
  if (values.includes(row.damage)) {
    return "\u4e0e\u4fdd\u62a40\u91cd\u53e0";
  }
  if (row.damage < Math.max(...values)) {
    return "\u4f4e\u4e8e\u4fdd\u62a40\u533a\u95f4";
  }
  return "\u9ad8\u4e8e\u4fdd\u62a40\u533a\u95f4";
}

function freqText(values) {
  const sorted = [...values].sort((a, b) => a - b);
  const unique = [...new Set(sorted)];
  return unique.map((value) => `${value}x${sorted.filter((item) => item === value).length}`).join(" ");
}

const logText = await fs.readFile(logPath, "utf8");
const rows = [];
const re = /^(?<stamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\.\d+ protection inferred: team=(?<team>\S+) level=(?<level>-?\d+) singleGuess=(?<singleGuess>-?\d+) samples=(?<samples>\d+) target=(?<target>.+?) observedDamage=(?<damage>\d+) health=(?<before>-?\d+)->(?<after>-?\d+) armor=(?<armor>\S+) armorPoints=(?<armorPoints>-?\d+) sword=(?<sword>\S+) sharp=(?<sharp>-?\d+) critical=(?<crit>\S+) predictedDamage=(?<predicted>\S+) error=(?<error>\S+)/;

for (const line of logText.split(/\r?\n/)) {
  const match = line.match(re);
  if (!match) {
    continue;
  }
  const g = match.groups;
  if (g.stamp <= cutoff) {
    continue;
  }
  const row = {
    stamp: g.stamp,
    time: g.stamp.slice(11),
    player: g.target,
    team: g.team,
    armor: g.armor,
    armorCn: armorNames[g.armor] ?? g.armor,
    armorPoints: Number(g.armorPoints),
    sword: g.sword,
    swordCn: swordNames[g.sword] ?? g.sword,
    sharp: Number(g.sharp),
    sharpCn: Number(g.sharp) > 0 ? "\u6709" : "\u65e0",
    crit: g.crit === "true",
    critCn: g.crit === "true" ? "\u662f" : "\u5426",
    damage: Number(g.damage),
    before: Number(g.before),
    after: Number(g.after),
    level: Number(g.level),
    singleGuess: Number(g.singleGuess),
    samples: Number(g.samples),
    predicted: g.predicted,
    error: g.error,
  };
  row.baseline = baselineStatus(row);
  rows.push(row);
}

const groups = new Map();
for (const row of rows) {
  const key = [row.armor, row.sword, row.sharp, row.crit].join("|");
  if (!groups.has(key)) {
    groups.set(key, []);
  }
  groups.get(key).push(row);
}

const armorOrder = new Map([["leather", 1], ["iron", 2], ["diamond", 3]]);
const swordOrder = new Map([["wood_sword", 1], ["gold_sword", 1], ["stone_sword", 2], ["iron_sword", 3], ["diamond_sword", 4]]);
const summaryRows = Array.from(groups.values())
  .map((items) => {
    const first = items[0];
    const damages = items.map((row) => row.damage).sort((a, b) => a - b);
    const unique = [...new Set(damages)];
    const zeroOverlap = items.filter((row) => row.baseline === "\u4e0e\u4fdd\u62a40\u91cd\u53e0").length;
    const lowerThanZero = items.filter((row) => row.baseline === "\u4f4e\u4e8e\u4fdd\u62a40\u533a\u95f4").length;
    return [
      first.armorCn,
      first.swordCn,
      first.sharpCn,
      first.critCn,
      items.length,
      Math.min(...damages),
      Math.max(...damages),
      unique.join(","),
      freqText(damages),
      zeroOverlap,
      lowerThanZero,
      items.map((row) => `${row.time}:${row.damage}`).join(" "),
    ];
  })
  .sort((a, b) => {
    const armorCmp = (armorOrder.get(Object.keys(armorNames).find((key) => armorNames[key] === a[0])) ?? 99)
      - (armorOrder.get(Object.keys(armorNames).find((key) => armorNames[key] === b[0])) ?? 99);
    if (armorCmp !== 0) return armorCmp;
    return (swordOrder.get(Object.keys(swordNames).find((key) => swordNames[key] === a[1])) ?? 99)
      - (swordOrder.get(Object.keys(swordNames).find((key) => swordNames[key] === b[1])) ?? 99);
  });

await fs.mkdir(outputDir, { recursive: true });
await fs.writeFile(rawJsonPath, JSON.stringify(rows, null, 2), "utf8");

const workbook = Workbook.create();
const summary = workbook.worksheets.add("\u4fdd\u62a41\u6c47\u603b");
const raw = workbook.worksheets.add("\u539f\u59cb\u6837\u672c");
summary.showGridLines = false;
raw.showGridLines = false;

summary.getRange("A1:L1").merge();
summary.getRange("A1").values = [["\u4fdd\u62a41\u8fde\u51fb\u4f24\u5bb3\u8868"]];
summary.getRange("A2:L2").merge();
summary.getRange("A2").values = [[`\u6570\u636e\u6765\u6e90\uff1a${logPath}\uff1b\u63d0\u53d6\u8303\u56f4\uff1a${cutoff} \u4e4b\u540e\uff1b\u6837\u672c ${rows.length} \u6761`]];
summary.getRange("A4:L4").values = [[
  "\u5957\u88c5",
  "\u5251",
  "\u950b\u5229",
  "\u66b4\u51fb",
  "\u6837\u672c\u6570",
  "\u6700\u5c0f\u4f24\u5bb3",
  "\u6700\u5927\u4f24\u5bb3",
  "\u4f24\u5bb3\u503c\u96c6\u5408",
  "\u9891\u6b21",
  "\u4e0e\u4fdd\u62a40\u91cd\u53e0\u6570",
  "\u4f4e\u4e8e\u4fdd\u62a40\u6570",
  "\u65f6\u95f4\u70b9",
]];
if (summaryRows.length > 0) {
  summary.getRange(`A5:L${summaryRows.length + 4}`).values = summaryRows;
}

raw.getRange("A1:Q1").values = [[
  "\u65f6\u95f4",
  "\u73a9\u5bb6",
  "\u961f\u4f0d",
  "\u5957\u88c5",
  "\u62a4\u7532\u70b9",
  "\u5251",
  "\u950b\u5229",
  "\u66b4\u51fb",
  "\u4f24\u5bb3",
  "\u524d\u8840\u91cf",
  "\u540e\u8840\u91cf",
  "\u56e2\u961f\u63a8\u7b97",
  "\u5355\u6b21\u63a8\u7b97",
  "\u6837\u672c\u6570",
  "\u76f8\u5bf9\u4fdd\u62a40",
  "\u9884\u6d4b\u4f24\u5bb3",
  "\u8bef\u5dee",
]];
if (rows.length > 0) {
  raw.getRange(`A2:Q${rows.length + 1}`).values = rows.map((row) => [
    row.time,
    row.player,
    row.team,
    row.armorCn,
    row.armorPoints,
    row.swordCn,
    row.sharpCn,
    row.critCn,
    row.damage,
    row.before,
    row.after,
    row.level,
    row.singleGuess,
    row.samples,
    row.baseline,
    row.predicted,
    row.error,
  ]);
}

summary.getRange("A1:L1").format = {
  fill: "#111827",
  font: { bold: true, color: "#FFFFFF", size: 16 },
  horizontalAlignment: "center",
};
summary.getRange("A2:L2").format = {
  fill: "#E0F2FE",
  font: { color: "#0F172A" },
  wrapText: true,
};
summary.getRange("A4:L4").format = {
  fill: "#0F766E",
  font: { bold: true, color: "#FFFFFF" },
  horizontalAlignment: "center",
};
if (summaryRows.length > 0) {
  summary.getRange(`A4:L${summaryRows.length + 4}`).format.borders = { preset: "all", style: "thin", color: "#CBD5E1" };
  summary.getRange(`E5:G${summaryRows.length + 4}`).format.numberFormat = "#,##0";
  summary.getRange(`J5:K${summaryRows.length + 4}`).format.numberFormat = "#,##0";
  summary.getRange(`A5:D${summaryRows.length + 4}`).format.horizontalAlignment = "center";
  summary.getRange(`H5:L${summaryRows.length + 4}`).format.wrapText = true;
}
summary.getRange("A:L").format.autofitColumns();
summary.getRange("A1:L2").format.rowHeight = 28;
summary.freezePanes.freezeRows(4);

raw.getRange("A1:Q1").format = {
  fill: "#334155",
  font: { bold: true, color: "#FFFFFF" },
  horizontalAlignment: "center",
};
if (rows.length > 0) {
  raw.getRange(`A1:Q${rows.length + 1}`).format.borders = { preset: "all", style: "thin", color: "#E2E8F0" };
  raw.getRange(`E2:E${rows.length + 1}`).format.numberFormat = "#,##0";
  raw.getRange(`I2:N${rows.length + 1}`).format.numberFormat = "#,##0";
}
raw.getRange("A:Q").format.autofitColumns();
raw.freezePanes.freezeRows(1);

if (summaryRows.length > 0) {
  summary.tables.add(`A4:L${summaryRows.length + 4}`, true, "ProtectionOneSummary");
}
if (rows.length > 0) {
  raw.tables.add(`A1:Q${rows.length + 1}`, true, "ProtectionOneRaw");
}

const inspect = await workbook.inspect({
  kind: "table",
  sheetId: "\u4fdd\u62a41\u6c47\u603b",
  range: `A4:L${Math.max(summaryRows.length + 4, 5)}`,
  include: "values",
  tableMaxRows: 20,
  tableMaxCols: 12,
});
console.log(inspect.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 50 },
  summary: "formula error scan",
});
console.log(errors.ndjson);

const preview = await workbook.render({
  sheetName: "\u4fdd\u62a41\u6c47\u603b",
  autoCrop: "all",
  scale: 1,
  format: "png",
});
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
await fs.copyFile(outputPath, desktopPath);
console.log(JSON.stringify({ rows: rows.length, outputPath, desktopPath, rawJsonPath, previewPath }, null, 2));
