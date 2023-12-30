package dev.dexsr.klio.base.compose

import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment

fun horizontalBiasAlignment(bias: Float): Alignment.Horizontal {
	require(bias in -1f..1f) {
		"BiasAlignment value=$bias must be in -1f..1f"
	}
	return BiasAlignment.Horizontal(bias)
}

fun verticalBiasAlignment(bias: Float): Alignment.Vertical {
	require(bias in -1f..1f) {
		"BiasAlignment value=$bias must be in -1f..1f"
	}
	return BiasAlignment.Vertical(bias)
}

fun biasAlignment(bias: Float): Alignment {
	Alignment.Center
	require(bias in -1f..1f) {
		"BiasAlignment value=$bias must be in -1f..1f"
	}
	return BiasAlignment(bias, bias)
}

fun biasAlignment(x: Float, y: Float): Alignment {
	Alignment.Center
	require(x in -1f..1f) {
		"BiasAlignment x=$x must be in -1f..1f"
	}
	require(y in -1f..1f) {
		"BiasAlignment y=$y must be in -1f..1f"
	}
	return BiasAlignment(x, y)
}
