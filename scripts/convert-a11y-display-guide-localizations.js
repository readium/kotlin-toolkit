/**
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 *
 * This script can be used to convert the localized files from https://github.com/w3c/publ-a11y-display-guide-localizations
 * into other output formats for various platforms.
 */

const fs = require('fs');
const path = require('path');
const [inputFolder, outputFormat, outputFolder, keyPrefix = ''] = process.argv.slice(2);

/**
 * Ends the script with the given error message.
 */
function fail(message) {
    console.error(`Error: ${message}`);
    process.exit(1);
}

/**
 * Converter for the KMP `readium-shared` module.
 *
 * The output folder must be the `src` directory of the module (e.g. `readium/shared/src`): the
 * Android string resources are written to `androidMain/res`, and the generated Kotlin sources are
 * split between `commonMain` (keys), `androidMain` (R.string resolution) and `iosMain` (bundled
 * en-US strings).
 */
function convertAndroid(lang, version, keys, keyPrefix, write) {
    const isBaseLanguage = (lang == 'en' || lang == 'en-US');
    const disclaimer = `DO NOT EDIT. File generated automatically from v${version} of the ${lang} JSON strings.`;

    let output = `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${disclaimer} -->\n\n<resources>\n`;
    for (const [key, value] of Object.entries(keys)) {
        const sanitizedKey = key.replace(/-/g, '_');
        output += `    <string name="${keyPrefix}${sanitizedKey}">${value.replace("'", "\\'")}</string>\n`;
    }
    output += '</resources>\n';

    let folder = 'androidMain/res/values';
    if (!isBaseLanguage) {
        folder += `-${lang}`;
    }

    let outputPath = path.join(folder, 'w3c_a11y_meta_display_guide_strings.xml');
    write(outputPath, output);

    // Using the "base" language, we will generate the static list of string keys (validated at
    // compile time) and the platform-specific resolution of the localized strings.
    if (isBaseLanguage) {
        writeKotlinExtensions(disclaimer, keys, keyPrefix, write);
    }
}

/**
 * Generates the Kotlin sources for the KMP source sets:
 *
 * - `commonMain`: the static list of string keys, to validate them at compile time.
 * - `androidMain`: the resolution of a key to its `R.string` resources.
 * - `iosMain`: a lookup table bundling the en-US strings.
 */
function writeKotlinExtensions(disclaimer, keys, keyPrefix, write) {
    const header = `/*
 *  Copyright 2025 Readium Foundation. All rights reserved.
 *  Use of this source code is governed by the BSD-style license
 *  available in the top-level LICENSE file of the project.
 */

// ${disclaimer}

package org.readium.r2.shared.accessibility

`;
    const kotlinDir = 'kotlin/org/readium/r2/shared/accessibility';

    let keysList = Object.keys(keys)
        .filter((k) => k.endsWith("-compact"))
        .map((k) => removeSuffix(k, "-compact"));

    // commonMain: string keys.
    let commonOutput = header;
    for (const key of keysList) {
        const sanitizedKey = key.replace(/-/g, '_');
        commonOutput += `internal val AccessibilityDisplayString.Companion.${convertKebabToUpperSnakeCase(key)}: AccessibilityDisplayString get() = AccessibilityDisplayString(key = "${sanitizedKey}")\n`;
    }
    write(path.join('commonMain', kotlinDir, 'AccessibilityDisplayString.kt'), commonOutput);

    // androidMain: R.string resolution.
    let androidOutput = header.replace(
        'package org.readium.r2.shared.accessibility\n',
        `package org.readium.r2.shared.accessibility

import android.content.Context
import androidx.annotation.StringRes
import org.readium.r2.shared.R
`
    );
    androidOutput += `/**
 * Returns the localized string for this display string.
 *
 * @param descriptive When true, will return the long descriptive statement.
 */
internal fun AccessibilityDisplayString.localizedString(context: Context, descriptive: Boolean): String =
    context.getString(resourceId(descriptive)).trim()

@StringRes
private fun AccessibilityDisplayString.resourceId(descriptive: Boolean): Int =
    when (key) {
`;
    for (const key of keysList) {
        const sanitizedKey = key.replace(/-/g, '_');
        androidOutput += `        "${sanitizedKey}" -> if (descriptive) R.string.${keyPrefix}${sanitizedKey}_descriptive else R.string.${keyPrefix}${sanitizedKey}_compact\n`;
    }
    androidOutput += `        else -> throw IllegalArgumentException("Unknown accessibility display string: $key")
    }
`;
    write(path.join('androidMain', kotlinDir, 'AccessibilityDisplayString.android.kt'), androidOutput);

    // iosMain: bundled en-US strings.
    let iosOutput = header;
    iosOutput += `/**
 * Returns the localized string for this display string.
 *
 * On iOS, only the en-US strings of the W3C display guide are bundled.
 *
 * @param descriptive When true, will return the long descriptive statement.
 */
internal fun AccessibilityDisplayString.localizedString(descriptive: Boolean): String =
    requireNotNull(
        accessibilityDisplayStrings[key + if (descriptive) "_descriptive" else "_compact"]
    ) { "Unknown accessibility display string: $key" }.trim()

/**
 * The en-US strings of the W3C accessibility metadata display guide, keyed by resource name
 * (without the ${'`'}${keyPrefix}${'`'} prefix).
 */
internal val accessibilityDisplayStrings: Map<String, String> = mapOf(
`;
    for (const [key, value] of Object.entries(keys)) {
        const sanitizedKey = key.replace(/-/g, '_');
        iosOutput += `    "${sanitizedKey}" to\n        "${escapeKotlinString(value)}",\n`;
    }
    iosOutput += ')\n';
    write(path.join('iosMain', kotlinDir, 'AccessibilityDisplayString.ios.kt'), iosOutput);
}

/**
 * Escapes a raw string for inclusion in a Kotlin string literal.
 */
function escapeKotlinString(value) {
    return value
        .replace(/\\/g, '\\\\')
        .replace(/"/g, '\\"')
        .replace(/\$/g, '\\$')
        .replace(/\n/g, '\\n');
}

const converters = {
    android: convertAndroid
};

if (!inputFolder || !outputFormat || !outputFolder) {
    console.error('Usage: node convert.js <input-folder> <output-format> <src-output-folder> [key-prefix]');
    console.error('For the android format, the output folder must be the module src directory (e.g. readium/shared/src).');
    process.exit(1);
}

const langFolder = path.join(inputFolder, 'lang');
if (!fs.existsSync(langFolder)) {
    fail(`the specified input folder does not contain a 'lang' directory`);
}

const convert = converters[outputFormat];
if (!convert) {
    fail(`unrecognized output format: ${outputFormat}, try: ${Object.keys(converters).join(', ')}.`);
}

fs.readdir(langFolder, (err, langDirs) => {
    if (err) {
        fail(`reading directory: ${err.message}`);
    }

    langDirs.forEach(langDir => {
        const langDirPath = path.join(langFolder, langDir);

        fs.readdir(langDirPath, (err, files) => {
            if (err) {
                fail(`reading language directory ${langDir}: ${err.message}`);
            }

            files.forEach(file => {
                const filePath = path.join(langDirPath, file);
                if (path.extname(file) === '.json') {
                    fs.readFile(filePath, 'utf8', (err, data) => {
                        if (err) {
                            console.error(`Error reading file ${file}: ${err.message}`);
                            return;
                        }

                        try {
                            const jsonData = JSON.parse(data);
                            const version = jsonData["metadata"]["version"];
                            convert(langDir, version, parseJsonKeys(jsonData), keyPrefix, write);
                        } catch (err) {
                            fail(`parsing JSON from file ${file}: ${err.message}`);
                        }
                    });
                }
            });
        });
    });
});

/**
 * Writes the given content to the file path relative to the outputFolder provided in the CLI arguments.
 */
function write(relativePath, content) {
    const outputPath = path.join(outputFolder, relativePath);
    const outputDir = path.dirname(outputPath);

    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }

    fs.writeFile(outputPath, content, 'utf8', err => {
        if (err) {
            fail(`writing file ${outputPath}: ${err.message}`);
        } else {
            console.log(`Wrote ${outputPath}`);
        }
    });
}

/**
 * Collects the JSON translation keys.
 */
function parseJsonKeys(obj) {
    const keys = {};
    for (const key in obj) {
        if (key === 'metadata') continue; // Ignore the metadata key
        if (typeof obj[key] === 'object') {
            for (const subKey in obj[key]) {
                if (typeof obj[key][subKey] === 'object') {
                    for (const innerKey in obj[key][subKey]) {
                        const fullKey = `${subKey}-${innerKey}`;
                        keys[fullKey] = obj[key][subKey][innerKey];
                    }
                } else {
                    keys[subKey] = obj[key][subKey];
                }
            }
        }
    }
    return keys;
}

function convertKebabToUpperSnakeCase(string) {
    return string
        .split('-')
        .map(word => word.toUpperCase())
        .join('_');
}

function removeSuffix(str, suffix) {
    if (str.endsWith(suffix)) {
        return str.slice(0, -suffix.length);
    }
    return str;
}