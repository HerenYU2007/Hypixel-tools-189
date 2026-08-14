import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = "C:\\Users\\97105\\Documents\\1.8.9";
const outputDir = path.join(root, "outputs", "protection-compare");
const protection1RawPath = path.join(root, "outputs", "protection1", "protection1-raw.json");
const outputPath = path.join(outputDir, "protection0-vs-protection1-comparison.xlsx");
const previewPath = path.join(outputDir, "protection0-vs-protection1-comparison-preview.png");
const desktopPath = "C:\\Users\\97105\\Desktop\\保护0-vs-保护1对比表.xlsx";

const armorNames = {
  leather: "皮革套",
  iron: "铁套",
  diamond: "钻套",
};

const swordNames = {
  wood_sword: "木剑",
  stone_sword: "石剑",
  iron_sword: "铁剑",
  diamond_sword: "钻石剑",
};

const armorOrder = ["leather", "iron", "diamond"];
const swordOrder = ["wood_sword", "stone_sword", "iron_sword", "diamond_sword"];

const protection0 = new Map(Object.entries({
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

function sortedUnique(values) {
  return [...new Set(values)].sort((a, b) => a - b);
}

function join(values) {
  return values.length ? values.join(",") : "-";
}

function freq(values) {
  const sorted = [...values].sort((a, b) => a - b);
  return sortedUnique(sorted).map((value) => `${value}x${sorted.filter((item) => item === value).length}`).join(" ");
}

const protection1Rows = JSON.parse(await fs.readFile(protection1RawPath, "utf8"));
const protection1Groups = new Map();
for (const row of protection1Rows) {
  if (row.sharp !== 0 || row.crit !== false) {
    continue;
  }
  const key = `${row.armor}|${row.sword}|${row.sharp}|${row.crit}`;
  if (!protection1Groups.has(key)) {
    protection1Groups.set(key, []);
  }
  protection1Groups.get(key).push(row.damage);
}

const comparisonRows = [];
for (const armor of armorOrder) {
  for (const sword of swordOrder) {
    const key = `${armor}|${sword}|0|false`;
    const zeroValues = sortedUnique(protection0.get(key) ?? []);
    const oneSamples = protection1Groups.get(key) ?? [];
    const oneValues = sortedUnique(oneSamples);
    const zeroMax = zeroValues.length ? Math.max(...zeroValues) : null;
    const overlap = oneValues.filter((value) => zeroValues.includes(value));
    const oneOnly = oneValues.filter((value) => !zeroValues.includes(value));
    const oneOnlyLower = oneOnly.filter((value) => zeroMax !== null && value < zeroMax);
    const oneOnlyHigher = oneOnly.filter((value) => zeroMax !== null && value > zeroMax);
    const lowerSampleCount = oneSamples.filter((value) => oneOnlyLower.includes(value)).length;
    const overlapSampleCount = oneSamples.filter((value) => overlap.includes(value)).length;
    const confidence = oneSamples.length ? lowerSampleCount / oneSamples.length : 0;
    let suggestion;
    if (confidence >= 0.45) {
      suggestion = "可用：连续样本出现低伤害即可判保护1";
    } else if (confidence >= 0.2) {
      suggestion = "谨慎：需要多次样本投票";
    } else {
      suggestion = "不稳：和保护0重叠太多";
    }
    comparisonRows.push([
      armorNames[armor],
      swordNames[sword],
      join(zeroValues),
      join(oneValues),
      join(overlap),
      join(oneOnlyLower),
      join(oneOnlyHigher),
      zeroValues.length,
      oneSamples.length,
      overlapSampleCount,
      lowerSampleCount,
      confidence,
      freq(oneSamples),
      suggestion,
    ]);
  }
}

const workbook = Workbook.create();
const sheet = workbook.worksheets.add("保护对比");
sheet.showGridLines = false;

sheet.getRange("A1:N1").merge();
sheet.getRange("A1").values = [["保护0 vs 保护1 连击伤害对比表"]];
sheet.getRange("A2:N2").merge();
sheet.getRange("A2").values = [[`保护0来自基础表；保护1来自 ${protection1Rows.length} 条新日志样本。低伤害特征 = 保护1出现、保护0没出现、且低于保护0最大值。`]];
sheet.getRange("A4:N4").values = [[
  "套装",
  "剑",
  "保护0伤害集合",
  "保护1伤害集合",
  "共同伤害",
  "保护1特有低伤害",
  "保护1特有高伤害",
  "保护0值数",
  "保护1样本数",
  "重叠样本数",
  "低伤害样本数",
  "低伤害占比",
  "保护1频次",
  "算法建议",
]];
sheet.getRange(`A5:N${comparisonRows.length + 4}`).values = comparisonRows;

sheet.getRange("A1:N1").format = {
  fill: "#111827",
  font: { bold: true, color: "#FFFFFF", size: 16 },
  horizontalAlignment: "center",
};
sheet.getRange("A2:N2").format = {
  fill: "#E0F2FE",
  font: { color: "#0F172A" },
  wrapText: true,
};
sheet.getRange("A4:N4").format = {
  fill: "#0F766E",
  font: { bold: true, color: "#FFFFFF" },
  horizontalAlignment: "center",
};
sheet.getRange(`A4:N${comparisonRows.length + 4}`).format.borders = {
  preset: "all",
  style: "thin",
  color: "#CBD5E1",
};
sheet.getRange(`H5:L${comparisonRows.length + 4}`).format.numberFormat = "#,##0";
sheet.getRange(`L5:L${comparisonRows.length + 4}`).format.numberFormat = "0.0%";
sheet.getRange(`A5:B${comparisonRows.length + 4}`).format.horizontalAlignment = "center";
sheet.getRange(`C5:G${comparisonRows.length + 4}`).format.wrapText = true;
sheet.getRange(`M5:N${comparisonRows.length + 4}`).format.wrapText = true;
sheet.getRange("A:N").format.autofitColumns();
sheet.getRange("A1:N2").format.rowHeight = 30;
sheet.freezePanes.freezeRows(4);
sheet.tables.add(`A4:N${comparisonRows.length + 4}`, true, "ProtectionCompare");

await fs.mkdir(outputDir, { recursive: true });
const inspect = await workbook.inspect({
  kind: "table",
  sheetId: "保护对比",
  range: `A4:N${comparisonRows.length + 4}`,
  include: "values",
  tableMaxRows: 20,
  tableMaxCols: 14,
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
  sheetName: "保护对比",
  autoCrop: "all",
  scale: 1,
  format: "png",
});
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
await fs.copyFile(outputPath, desktopPath);
console.log(JSON.stringify({ outputPath, desktopPath, previewPath }, null, 2));
