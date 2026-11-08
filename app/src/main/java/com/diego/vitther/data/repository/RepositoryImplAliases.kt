package com.diego.vitther.data.repository

import com.diego.vitther.data.local.CapsulaDao
import com.diego.vitther.data.local.PiezaDao
import java.time.Clock

/**
 * Alias de implementación (*RepositoryImpl) en data/repository/ que cumplen con los
 * contratos de domain/repository/ definidos en la guía técnica MVVM.
 */
typealias PiezaRepositoryImpl = PiezaRepository
typealias CapsulaRepositoryImpl = CapsulaRepository
