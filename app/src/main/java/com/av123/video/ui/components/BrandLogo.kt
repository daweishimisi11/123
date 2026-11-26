package com.av123.video.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 站点 --color-gradient 两端色（品牌渐变，固定色，不随动态取色变化） */
private val brandGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF8A5C), Color(0xFFFF4D8D))
)

/**
 * 品牌字标，显示为渐变「123」与尾部装饰句号「.」：
 * - “123”：品牌渐变（135° #ff8a5c → #ff4d8d）；
 * - 尾部「.」：同色系句号基线点缀。
 * 字重 ExtraBold、字距 -0.03em，风格一致。
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    val tightSpacing = (-0.03f * style.fontSize.value).sp
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "123",
            style = style.copy(
                brush = brandGradient,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = tightSpacing
            )
        )
        // 尾部就是句号本身：直接用「.」字符，天然落在基线上、随字号缩放，
        // 不需要再手算偏移（用 Box 时要么飘到字上方、要么突出底边）
        Text(
            text = ".",
            style = style.copy(
                brush = brandGradient,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            )
        )
    }
}
