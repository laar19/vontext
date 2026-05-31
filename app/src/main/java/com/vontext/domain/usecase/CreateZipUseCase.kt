package com.vontext.domain.usecase

import com.vontext.processor.ZipCreator
import java.io.File
import javax.inject.Inject

class CreateZipUseCase @Inject constructor(
    private val zipCreator: ZipCreator
) {
    suspend operator fun invoke(outputDir: File): Result<File> {
        return zipCreator.create(outputDir)
    }
}
