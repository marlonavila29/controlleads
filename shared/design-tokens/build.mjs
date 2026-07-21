#!/usr/bin/env node
/**
 * Design tokens build — single source (tokens.json) → both clients:
 *   web/src/styles/_tokens.scss   (CSS custom properties + SCSS map)
 *   app/lib/theme/tokens.dart     (Dart constants for ThemeData)
 *
 * Zero dependencies on purpose — runs anywhere Node runs.
 * Usage: node shared/design-tokens/build.mjs
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const tokens = JSON.parse(readFileSync(join(root, 'shared/design-tokens/tokens.json'), 'utf8'));

// Flatten {a:{b:{value:x}}} → [['a-b', x], ...], skipping $-prefixed metadata keys
function flatten(obj, prefix = []) {
  const out = [];
  for (const [key, val] of Object.entries(obj)) {
    if (key.startsWith('$')) continue;
    if (val && typeof val === 'object' && 'value' in val) {
      out.push([[...prefix, key].join('-'), val.value]);
    } else if (val && typeof val === 'object') {
      out.push(...flatten(val, [...prefix, key]));
    }
  }
  return out;
}

const flat = flatten(tokens);

// ---------- SCSS (web) ----------
const numericUnitless = new Set(['font-weight']);
const pxCategories = ['font-size', 'space-', 'radius-'];
function scssValue(name, value) {
  if (/^\d+(\.\d+)?$/.test(String(value))) {
    if (name.startsWith('motion-duration')) return `${value}ms`;
    if (pxCategories.some((c) => name.includes(c) || name.startsWith(c))) return `${value}px`;
    if ([...numericUnitless].some((c) => name.includes(c))) return value;
    return `${value}px`;
  }
  return value;
}

const cssVars = flat.map(([n, v]) => `  --cl-${n}: ${scssValue(n, v)};`).join('\n');
const scss = `// GENERATED from shared/design-tokens/tokens.json — do not edit by hand.
// Rebuild: node shared/design-tokens/build.mjs

:root {
${cssVars}
}
`;
mkdirSync(join(root, 'web/src/styles'), { recursive: true });
writeFileSync(join(root, 'web/src/styles/_tokens.scss'), scss);

// ---------- Dart (app) ----------
function dartName(name) {
  return name.replace(/-(\w)/g, (_, c) => c.toUpperCase());
}
const dartLines = flat.map(([n, v]) => {
  const id = dartName(n);
  if (/^#[0-9A-Fa-f]{6}$/.test(String(v))) {
    return `  static const Color ${id} = Color(0xFF${String(v).slice(1).toUpperCase()});`;
  }
  if (/^\d+(\.\d+)?$/.test(String(v))) {
    if (n.startsWith('motion-duration')) {
      return `  static const Duration ${id} = Duration(milliseconds: ${v});`;
    }
    return `  static const double ${id} = ${v};`;
  }
  return `  static const String ${id} = ${JSON.stringify(String(v))};`;
});
const dart = `// GENERATED from shared/design-tokens/tokens.json — do not edit by hand.
// Rebuild: node shared/design-tokens/build.mjs
import 'package:flutter/material.dart';

abstract final class ClTokens {
${dartLines.join('\n')}
}
`;
mkdirSync(join(root, 'app/lib/theme'), { recursive: true });
writeFileSync(join(root, 'app/lib/theme/tokens.dart'), dart);

console.log(`✓ ${flat.length} tokens → web/src/styles/_tokens.scss + app/lib/theme/tokens.dart`);
