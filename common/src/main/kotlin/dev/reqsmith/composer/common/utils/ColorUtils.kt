/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2025-2026. Kovi <kovihome86@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.reqsmith.composer.common.utils

import kotlin.math.*

data class LabColor(val L: Double, val a: Double, val b: Double)

data class RGBColor(val r: Int, val g: Int, val b: Int)

object ColorUtils {

    private fun rgbToLab(color: RGBColor): LabColor {
        // 1. RGB [0,255] → XYZ
        val (rr, gg, bb) = listOf(color.r, color.g, color.b).map { c ->
            val cLinear = c / 255.0
            if (cLinear > 0.04045)
                ((cLinear + 0.055) / 1.055).pow(2.4)
            else
                cLinear / 12.92
        }

        val x = (rr * 0.4124 + gg * 0.3576 + bb * 0.1805) / 0.95047
        val y = (rr * 0.2126 + gg * 0.7152 + bb * 0.0722) / 1.00000
        val z = (rr * 0.0193 + gg * 0.1192 + bb * 0.9505) / 1.08883

        // 2. XYZ → Lab
        fun f(t: Double) =
            if (t > 0.008856) t.pow(1.0 / 3) else (7.787 * t) + (16.0 / 116)

        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        val L = 116 * fy - 16
        val a = 500 * (fx - fy)
        val b = 200 * (fy - fz)

        return LabColor(L, a, b)
    }

    /**
     * Calculate distance of two RGB colors using CIEDE2000 standard
     *
     * @param c1rgb First RGB color
     * @param c2rgb Second RGB color
     * @return The CIEDE2000 distance of the two
     */
    fun ciede2000(c1rgb: RGBColor, c2rgb: RGBColor): Double {
        val c1 = rgbToLab(c1rgb)
        val c2 = rgbToLab(c2rgb)

        val avgLp = (c1.L + c2.L) / 2.0
        val C1 = sqrt(c1.a * c1.a + c1.b * c1.b)
        val C2 = sqrt(c2.a * c2.a + c2.b * c2.b)
        val avgC = (C1 + C2) / 2.0

        val G = 0.5 * (1 - sqrt((avgC.pow(7)) / (avgC.pow(7) + 25.0.pow(7))))
        val a1p = c1.a * (1 + G)
        val a2p = c2.a * (1 + G)

        val C1p = sqrt(a1p * a1p + c1.b * c1.b)
        val C2p = sqrt(a2p * a2p + c2.b * c2.b)

        val avgCp = (C1p + C2p) / 2.0

        val h1p = atan2(c1.b, a1p).let { if (it >= 0) it else it + 2 * PI } * 180 / PI
        val h2p = atan2(c2.b, a2p).let { if (it >= 0) it else it + 2 * PI } * 180 / PI

        val deltahp = when {
            abs(h1p - h2p) <= 180 -> h2p - h1p
            h2p <= h1p -> h2p - h1p + 360
            else -> h2p - h1p - 360
        }

        val deltaLp = c2.L - c1.L
        val deltaCp = C2p - C1p
        val deltaHp = 2 * sqrt(C1p * C2p) * sin((deltahp / 2) * PI / 180)

        val avgHp = when {
            abs(h1p - h2p) > 180 -> (h1p + h2p + 360) / 2
            else -> (h1p + h2p) / 2
        }

        val T = 1 - 0.17 * cos((avgHp - 30) * PI / 180) +
                0.24 * cos((2 * avgHp) * PI / 180) +
                0.32 * cos((3 * avgHp + 6) * PI / 180) -
                0.20 * cos((4 * avgHp - 63) * PI / 180)

        val deltaTheta = 30 * exp(-((avgHp - 275) / 25).pow(2))
        val Rc = 2 * sqrt(avgCp.pow(7) / (avgCp.pow(7) + 25.0.pow(7)))
        val Sl = 1 + ((0.015 * (avgLp - 50).pow(2)) / sqrt(20 + (avgLp - 50).pow(2)))
        val Sc = 1 + 0.045 * avgCp
        val Sh = 1 + 0.015 * avgCp * T
        val Rt = -sin(2 * deltaTheta * PI / 180) * Rc

        return sqrt(
            (deltaLp / Sl).pow(2) +
                    (deltaCp / Sc).pow(2) +
                    (deltaHp / Sh).pow(2) +
                    Rt * (deltaCp / Sc) * (deltaHp / Sh)
        )
    }

    /**
     * Calculate simple metric distance of two RGB colors
     *
     * @param c1 First RGB color
     * @param c2 Second RGB color
     * @return The metric distance of the two
     */
    fun metric(c1: RGBColor, c2: RGBColor): Double {
        return sqrt(
            ((c1.r - c2.r) * (c1.r - c2.r) +
                    (c1.g - c2.g) * (c1.g - c2.g) +
                    (c1.b - c2.b) * (c1.b - c2.b)).toDouble()
        )
    }

    /**
     * Convert css color string to RGB
     *
     * @param color css color string
     * @return RGB color
     */
    fun cssToRGB(color: String): RGBColor? {
        val hex = color.trim().lowercase()

        return when {
            hex.startsWith("#") -> {
                val clean = hex.removePrefix("#")
                when (clean.length) {
                    3 -> {
                        val r = Integer.parseInt("${clean[0]}${clean[0]}", 16)
                        val g = Integer.parseInt("${clean[1]}${clean[1]}", 16)
                        val b = Integer.parseInt("${clean[2]}${clean[2]}", 16)
                        RGBColor(r, g, b)
                    }
                    6 -> {
                        val r = Integer.parseInt(clean.substring(0, 2), 16)
                        val g = Integer.parseInt(clean.substring(2, 4), 16)
                        val b = Integer.parseInt(clean.substring(4, 6), 16)
                        RGBColor(r, g, b)
                    }
                    else -> null
                }
            }

            hex.startsWith("rgb(") && hex.endsWith(")") -> {
                val parts = hex.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toInt() }
                if (parts.size == 3) RGBColor(parts[0], parts[1], parts[2]) else null
            }

            else -> null // nem támogatott formátum
        }
    }

}

