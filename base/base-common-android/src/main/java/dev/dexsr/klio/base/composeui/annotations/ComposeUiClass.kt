package dev.dexsr.klio.base.composeui.annotations

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

//
// TODO: exclusive ?
//

/**
 * Denotes that a class is aware and follows the contract of Compose-UI
 *
 * - includes the contract and effect of @[Stable] on compose compiler
 */
@Stable
annotation class ComposeUiClass()

/**
 * Denotes that a class is aware and follows the contract of Compose-UI
 *
 * - includes the contract and effect of @[Immutable] on compose compiler
 */
@Immutable
annotation class ImmutableComposeUiClass()
