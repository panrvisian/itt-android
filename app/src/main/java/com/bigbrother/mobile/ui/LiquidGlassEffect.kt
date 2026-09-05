package com.bigbrother.mobile.ui

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged

/**
 * Adds a subtle optical edge distortion to the already blurred navigation backdrop.
 * Android 12 keeps the frosted Haze layer; Android 13+ also receives this AGSL lens pass.
 */
internal fun Modifier.liquidGlassLens(enabled: Boolean): Modifier {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    return then(liquidGlassLensApi33())
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun liquidGlassLensApi33(): Modifier = Modifier.composed {
    val shader = remember { RuntimeShader(LIQUID_GLASS_SHADER) }
    val effect = remember(shader) {
        AndroidRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
    Modifier
        .onSizeChanged { size ->
            shader.setFloatUniform("size", size.width.toFloat(), size.height.toFloat())
        }
        .graphicsLayer { renderEffect = effect }
}

private const val LIQUID_GLASS_SHADER = """
    uniform shader content;
    uniform float2 size;

    half4 main(float2 position) {
        float2 safeSize = max(size, float2(1.0));
        float2 uv = position / safeSize;
        float2 centered = uv * 2.0 - 1.0;
        centered.x *= safeSize.x / safeSize.y;

        float radius = length(centered);
        float edge = smoothstep(0.68, 1.02, radius);
        float2 direction = radius > 0.001 ? centered / radius : float2(0.0);
        float refraction = (1.0 - edge) * edge * 8.0;
        float2 samplePosition = position - direction * refraction;

        half4 color = content.eval(samplePosition);
        float upperRim = smoothstep(0.90, 0.68, radius) * smoothstep(0.45, 0.78, radius);
        upperRim *= smoothstep(0.45, -0.75, centered.y);
        color.rgb += half3(upperRim * 0.08);
        return color;
    }
"""
