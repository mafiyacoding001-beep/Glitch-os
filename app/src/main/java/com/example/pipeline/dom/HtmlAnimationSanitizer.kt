package com.example.pipeline.dom

import java.util.regex.Pattern

/**
 * Validates Notrack HTML output and wraps its body, styles, and script in a standalone document.
 */
object HtmlAnimationSanitizer {

    fun sanitizeAndExtractCode(rawOutput: String, scene: DomSceneScript): HtmlAnimationCode {
        var clean = rawOutput.trim()

        if (clean.contains("```")) {
            clean = clean.replace(Regex("```[a-zA-Z]*\\n?"), "").replace("```", "").trim()
        }

        require(clean.length <= 500_000) {
            "The animation provider response is too large to render safely."
        }
        require(Regex("<html\\b", RegexOption.IGNORE_CASE).containsMatchIn(clean)) {
            "The animation provider did not return a complete HTML document for scene ${scene.sceneNumber}."
        }

        val extractedHtml = extractTag(clean, "body")?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("The animation provider returned no HTML body for scene ${scene.sceneNumber}.")
        val extractedCss = extractTag(clean, "style")?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("The animation provider returned no CSS for scene ${scene.sceneNumber}.")
        val extractedJs = extractTag(clean, "script")?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("The animation provider returned no JavaScript for scene ${scene.sceneNumber}.")
        val bodyMarkup = extractedHtml
            .replace(
                Regex(
                    "<style\\b[^>]*>.*?</style>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                ),
                ""
            )
            .replace(
                Regex(
                    "<script\\b[^>]*>.*?</script>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                ),
                ""
            )
            .trim()
        require(bodyMarkup.isNotBlank()) {
            "The animation provider returned no scene markup for scene ${scene.sceneNumber}."
        }
        require(!Regex("<script\\b[^>]*\\bsrc\\s*=", RegexOption.IGNORE_CASE).containsMatchIn(clean)) {
            "External JavaScript is not allowed in scene ${scene.sceneNumber}."
        }
        require(!Regex("@import\\b|url\\(\\s*['\"]?https?://", RegexOption.IGNORE_CASE).containsMatchIn(extractedCss)) {
            "External CSS resources are not allowed in scene ${scene.sceneNumber}."
        }
        require(!Regex("<(?:img|iframe|video|audio|source)\\b[^>]*\\bsrc\\s*=\\s*['\"]https?://", RegexOption.IGNORE_CASE)
            .containsMatchIn(extractedHtml)) {
            "External media resources are not allowed in scene ${scene.sceneNumber}."
        }

        val fullHtmlDocument = assembleStandaloneDocument(
            scene = scene,
            htmlBody = bodyMarkup,
            cssStyle = extractedCss,
            jsLogic = extractedJs
        )

        return HtmlAnimationCode(
            html = bodyMarkup,
            css = extractedCss.trim(),
            javascript = extractedJs.trim(),
            fullHtmlDocument = fullHtmlDocument
        )
    }

    private fun extractTag(source: String, tagName: String): String? {
        val pattern = Pattern.compile("<$tagName[^>]*>(.*?)</$tagName>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(source)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else {
            null
        }
    }

    private fun assembleStandaloneDocument(
        scene: DomSceneScript,
        htmlBody: String,
        cssStyle: String,
        jsLogic: String
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=1080, height=1920, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <title>Scene ${scene.sceneNumber} - ${escapeHtml(scene.sceneTitle)}</title>
                <style>
                    $cssStyle
                </style>
            </head>
            <body>
                $htmlBody
                <script>
                    $jsLogic
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
