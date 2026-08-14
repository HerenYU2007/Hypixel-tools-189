import fs from "node:fs/promises";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const outputDir = "C:\\Users\\97105\\Documents\\1.8.9\\outputs\\protection0";
const rawJsonPath = `${outputDir}\\protection0-raw.json`;
const outputPath = `${outputDir}\\保护0连击伤害表.xlsx`;
const previewPath = `${outputDir}\\保护0连击伤害表-preview.png`;

const rawRows = JSON.parse((await fs.readFile(rawJsonPath, "utf8")).replace(/^\uFEFF/, ""));

const groups = new Map();
for (const row of rawRows) {
  const key = [row.armor, row.sword, row.sharp, row.crit].join("\u0001");
  if (!groups.has(key)) {
    groups.set(key, []);
  }
  groups.get(key).push(row);
}

const summaryRows = Array.from(groups.entries())
  .map(([key, rows]) => {
    const [armor, sword, sharp, crit] = key.split("\u0001");
    const damages = rows.map((row) => row.damage).sort((a, b) => a - b);
    const unique = [...new Set(damages)];
    const freq = unique.map((value) => `${value}x${damages.filter((item) => item === value).length}`).join(" ");
    return [
      armor,
      sword,
      sharp,
      crit,
      rows.length,
      rows.filter((row) => row.sample.startsWith("入样")).length,
      rows.filter((row) => !row.sample.startsWith("入样")).length,
      Math.min(...damages),
      Math.max(...damages),
      unique.join(","),
      freq,
    ];
  })
  .sort((a, b) => `${a[0]}-${a[1]}`.localeCompare(`${b[0]}-${b[1]}`, "zh-CN"));

const workbook = Workbook.create();
const summary = workbook.worksheets.add("保护0汇总");
const raw = workbook.worksheets.add("原始样本");
summary.showGridLines = false;
raw.showGridLines = false;

summary.getRange("A1:K1").merge();
summary.getRange("A1").values = [["保护0连击伤害表"]];
summary.getRange("A2:K2").merge();
summary.getRange("A2").values = [[`数据来源：Lunar latest.log，时间范围：2026-08-13 16:55:00 之后，有效数字伤害样本 ${rawRows.length} 条`]];
summary.getRange("A4:K4").values = [[
  "套装",
  "剑",
  "锋利",
  "暴击",
  "样本数",
  "入样数",
  "忽略数",
  "最小伤害",
  "最大伤害",
  "伤害值集合",
  "频次",
]];
summary.getRange(`A5:K${summaryRows.length + 4}`).values = summaryRows;

raw.getRange("A1:K1").values = [[
  "时间",
  "玩家",
  "套装",
  "剑",
  "锋利",
  "暴击",
  "伤害",
  "前血量",
  "后血量",
  "推算",
  "样本状态",
]];
raw.getRange(`A2:K${rawRows.length + 1}`).values = rawRows.map((row) => [
  row.time,
  row.player,
  row.armor,
  row.sword,
  row.sharp,
  row.crit,
  row.damage,
  row.before,
  row.after,
  row.guess,
  row.sample,
]);

const title = summary.getRange("A1:K1");
title.format = {
  fill: "#111827",
  font: { bold: true, color: "#FFFFFF", size: 16 },
  horizontalAlignment: "center",
};
summary.getRange("A2:K2").format = {
  fill: "#E0F2FE",
  font: { color: "#0F172A" },
  wrapText: true,
};
const header = summary.getRange("A4:K4");
header.format = {
  fill: "#0369A1",
  font: { bold: true, color: "#FFFFFF" },
  horizontalAlignment: "center",
};
summary.getRange(`A4:K${summaryRows.length + 4}`).format.borders = {
  preset: "all",
  style: "thin",
  color: "#CBD5E1",
};
summary.getRange(`E5:I${summaryRows.length + 4}`).format.numberFormat = "#,##0";
summary.getRange(`A5:D${summaryRows.length + 4}`).format.horizontalAlignment = "center";
summary.getRange(`E5:I${summaryRows.length + 4}`).format.horizontalAlignment = "right";
summary.getRange(`J5:K${summaryRows.length + 4}`).format.wrapText = true;
summary.getRange("A:K").format.autofitColumns();
summary.getRange("A1:K2").format.rowHeight = 28;
summary.freezePanes.freezeRows(4);

const rawHeader = raw.getRange("A1:K1");
rawHeader.format = {
  fill: "#334155",
  font: { bold: true, color: "#FFFFFF" },
  horizontalAlignment: "center",
};
raw.getRange(`A1:K${rawRows.length + 1}`).format.borders = {
  preset: "all",
  style: "thin",
  color: "#E2E8F0",
};
raw.getRange(`G2:I${rawRows.length + 1}`).format.numberFormat = "#,##0";
raw.getRange("A:K").format.autofitColumns();
raw.freezePanes.freezeRows(1);

summary.tables.add(`A4:K${summaryRows.length + 4}`, true, "ProtectionZeroSummary");
raw.tables.add(`A1:K${rawRows.length + 1}`, true, "ProtectionZeroRaw");

await fs.mkdir(outputDir, { recursive: true });
const inspect = await workbook.inspect({
  kind: "table",
  sheetId: "保护0汇总",
  range: `A4:K${summaryRows.length + 4}`,
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
  sheetName: "保护0汇总",
  autoCrop: "all",
  scale: 1,
  format: "png",
});
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
console.log(outputPath);
