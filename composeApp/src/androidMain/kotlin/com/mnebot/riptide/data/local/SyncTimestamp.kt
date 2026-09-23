package com.mnebot.riptide.data.local

import kotlin.time.Clock

/**
 * Marca de tiempo para el control de sincronización, SIEMPRE en UTC con sufijo 'Z'.
 *
 * No usar la zona local: el servidor compara `updatedAt` del cliente contra el suyo
 * para resolver conflictos, y dos relojes de pared en zonas distintas hacen que el
 * servidor rechace cambios del móvil (borrados que resucitan) o al revés.
 */
fun nowIso(): String = Clock.System.now().toString()
