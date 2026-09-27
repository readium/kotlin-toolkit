/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.fakes

import org.json.JSONObject

val validLicenseJsonStr = """
    {
      "provider": "ProviderName",
      "id": "doc_id_123",
      "issued": "2020-01-01T12:00:00Z",
      "updated": "2020-01-02T12:00:00Z",
      "encryption": {
        "profile": "http://readium.org/lcp/basic",
        "content_key": {
          "algorithm": "aes-256-cbc",
          "encrypted_value": "xxxx"
        },
        "user_key": {
          "text_hint": "Enter your password",
          "algorithm": "sha-256",
          "key_check": "yyyy"
        }
      },
      "links": [
        {
          "href": "http://example.com/hint",
          "rel": "hint"
        },
        {
          "href": "http://example.com/publication.epub",
          "rel": "publication"
        }
      ],
      "signature": {
        "algorithm": "sha-256",
        "certificate": "cert_content",
        "value": "sig_val"
      }
    }
""".trimIndent()

val validLicenseJson = JSONObject(validLicenseJsonStr)
