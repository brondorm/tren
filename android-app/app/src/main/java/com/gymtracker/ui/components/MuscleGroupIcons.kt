package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Данные об иконке и цвете для группы мышц
 */
data class MuscleGroupVisual(
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color
)

/**
 * Цвета для групп мышц (яркие, узнаваемые)
 */
object MuscleGroupColors {
    // Грудь - красный/розовый
    val chest = Color(0xFFE53935)
    val chestBg = Color(0xFFFFCDD2)

    // Спина - синий
    val back = Color(0xFF1E88E5)
    val backBg = Color(0xFFBBDEFB)

    // Плечи - зеленый
    val shoulders = Color(0xFF43A047)
    val shouldersBg = Color(0xFFC8E6C9)

    // Ноги - оранжевый
    val legs = Color(0xFFFB8C00)
    val legsBg = Color(0xFFFFE0B2)

    // Руки - фиолетовый
    val arms = Color(0xFF8E24AA)
    val armsBg = Color(0xFFE1BEE7)

    // Запасной цвет
    val default = Color(0xFF757575)
    val defaultBg = Color(0xFFEEEEEE)
}

/**
 * Получает визуальные данные (иконку и цвет) для группы мышц по названию
 */
fun getMuscleGroupVisual(muscleGroupName: String): MuscleGroupVisual {
    return when (muscleGroupName.lowercase()) {
        // Грудь
        "грудь" -> MuscleGroupVisual(
            icon = Icons.Filled.FitnessCenter,
            color = MuscleGroupColors.chest,
            backgroundColor = MuscleGroupColors.chestBg
        )

        // Спина и подгруппы
        "спина", "широчайшие", "трапеция" -> MuscleGroupVisual(
            icon = Icons.Filled.Accessibility,
            color = MuscleGroupColors.back,
            backgroundColor = MuscleGroupColors.backBg
        )

        // Плечи и подгруппы
        "плечи", "передняя дельта", "средняя дельта", "задняя дельта" -> MuscleGroupVisual(
            icon = Icons.Filled.SportsHandball,
            color = MuscleGroupColors.shoulders,
            backgroundColor = MuscleGroupColors.shouldersBg
        )

        // Ноги и подгруппы
        "ноги", "квадрицепс", "бицепс бедра", "икры" -> MuscleGroupVisual(
            icon = Icons.Filled.DirectionsRun,
            color = MuscleGroupColors.legs,
            backgroundColor = MuscleGroupColors.legsBg
        )

        // Руки и подгруппы
        "руки", "бицепс", "трицепс", "предплечья" -> MuscleGroupVisual(
            icon = Icons.Filled.FrontHand,
            color = MuscleGroupColors.arms,
            backgroundColor = MuscleGroupColors.armsBg
        )

        else -> MuscleGroupVisual(
            icon = Icons.Filled.Circle,
            color = MuscleGroupColors.default,
            backgroundColor = MuscleGroupColors.defaultBg
        )
    }
}

/**
 * Компонент иконки группы мышц с цветным фоном
 */
@Composable
fun MuscleGroupIcon(
    muscleGroupName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp
) {
    val visual = getMuscleGroupVisual(muscleGroupName)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(visual.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = muscleGroupName,
            tint = visual.color,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Маленькая иконка группы мышц (для использования в строках)
 */
@Composable
fun MuscleGroupIconSmall(
    muscleGroupName: String,
    modifier: Modifier = Modifier
) {
    MuscleGroupIcon(
        muscleGroupName = muscleGroupName,
        modifier = modifier,
        size = 32.dp,
        iconSize = 18.dp
    )
}

/**
 * Получает только цвет для группы мышц (для прогресс-баров и т.д.)
 */
fun getMuscleGroupColor(muscleGroupName: String): Color {
    return getMuscleGroupVisual(muscleGroupName).color
}
