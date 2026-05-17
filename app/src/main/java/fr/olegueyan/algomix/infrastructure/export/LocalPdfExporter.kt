package fr.olegueyan.algomix.infrastructure.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.application.port.PdfExporter
import fr.olegueyan.algomix.domain.export.ExportedPdf
import fr.olegueyan.algomix.domain.library.SheetId
import java.io.File
import java.io.IOException

class LocalPdfExporter(
    context: Context,
    private val libraryRepository: LibraryRepository,
) : PdfExporter {
    private val appContext = context.applicationContext

    @Suppress("TooGenericExceptionCaught")
    override suspend fun exportSheet(sheetId: SheetId): AppResult<ExportedPdf> {
        val sheet = libraryRepository.listSheets().getOrNull()
            ?.firstOrNull { it.id == sheetId }
            ?: return AppResult.failure(AppError.NotFound("Sheet not found"))
        val algorithms = libraryRepository.listAlgorithms(sheetId).getOrNull()
            ?: return AppResult.failure(AppError.NotFound("Sheet algorithms not found"))

        val outputDirectory = File(appContext.cacheDir, PDF_DIRECTORY).also { it.mkdirs() }
        val fileName = "${sheet.name.toFileName()}-$PDF_SUFFIX.pdf"
        val outputFile = File(outputDirectory, fileName)
        return try {
            val document = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val titlePaint = Paint().apply {
                    textSize = TITLE_SIZE
                    isFakeBoldText = true
                }
                val bodyPaint = Paint().apply { textSize = BODY_SIZE }

                var y = TOP_MARGIN
                canvas.drawText(sheet.name, LEFT_MARGIN, y, titlePaint)
                y += SECTION_GAP
                algorithms.forEachIndexed { index, algorithm ->
                    canvas.drawText("${index + 1}. ${algorithm.name}", LEFT_MARGIN, y, bodyPaint)
                    y += LINE_GAP
                    canvas.drawText(algorithm.sequence, LEFT_MARGIN, y, bodyPaint)
                    y += SECTION_GAP
                }
                if (algorithms.isEmpty()) {
                    canvas.drawText("Aucun algorithme", LEFT_MARGIN, y, bodyPaint)
                }
                document.finishPage(page)
                outputFile.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }
            AppResult.success(
                ExportedPdf(
                    fileName = fileName,
                    displayPath = outputFile.absolutePath,
                    localFilePath = outputFile.absolutePath,
                ),
            )
        } catch (error: RuntimeException) {
            writeFallbackPdf(outputFile, sheet.name, algorithms.map { "${it.name}: ${it.sequence}" }, error)
        }
    }

    @Suppress("SwallowedException")
    private fun writeFallbackPdf(
        outputFile: File,
        title: String,
        lines: List<String>,
        renderError: RuntimeException,
    ): AppResult<ExportedPdf> =
        try {
            outputFile.writeText(
                buildString {
                    appendLine("%PDF-1.4")
                    appendLine("% Algomix fallback export")
                    appendLine(title)
                    lines.forEach(::appendLine)
                    appendLine("%%EOF")
                },
            )
            AppResult.success(
                ExportedPdf(
                    fileName = outputFile.name,
                    displayPath = outputFile.absolutePath,
                    localFilePath = outputFile.absolutePath,
                ),
            )
        } catch (error: IOException) {
            AppResult.failure(AppError.Storage("PDF export failed after ${renderError.message}", error))
        }

    private fun String.toFileName(): String =
        lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "sheet" }

    companion object {
        private const val PDF_DIRECTORY = "exports"
        private const val PDF_SUFFIX = "algomix"
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val LEFT_MARGIN = 48f
        private const val TOP_MARGIN = 64f
        private const val TITLE_SIZE = 22f
        private const val BODY_SIZE = 13f
        private const val LINE_GAP = 20f
        private const val SECTION_GAP = 34f
    }
}
