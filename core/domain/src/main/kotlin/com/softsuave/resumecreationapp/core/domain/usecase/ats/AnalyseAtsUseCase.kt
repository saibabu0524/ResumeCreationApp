package com.softsuave.resumecreationapp.core.domain.usecase.ats

import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.AtsRepository
import com.softsuave.resumecreationapp.core.domain.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Analyses a resume PDF against a job description using the ATS scanner.
 *
 * Follows the project-wide [UseCase] pattern — the ViewModel calls
 * `analyseAtsUseCase(params)` and receives a [Result<AtsResult>].
 */
class AnalyseAtsUseCase @Inject constructor(
    private val atsRepository: AtsRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<AnalyseAtsUseCase.Params, AtsResult>(dispatcher) {

    override suspend fun execute(parameters: Params): Result<AtsResult> =
        atsRepository.analyseAts(
            pdfBytes = parameters.pdfBytes,
            fileName = parameters.fileName,
            jobDescription = parameters.jobDescription,
            provider = parameters.provider,
        )

    data class Params(
        val pdfBytes: ByteArray,
        val fileName: String,
        val jobDescription: String,
        val provider: String,
    )
}

