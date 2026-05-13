package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.export.ExportedPdf
import fr.olegueyan.algomix.domain.library.SheetId

interface PdfExporter {
    suspend fun exportSheet(sheetId: SheetId): AppResult<ExportedPdf>
}
