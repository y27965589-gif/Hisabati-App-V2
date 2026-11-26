package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CatalogItem
import com.example.ui.components.BadgeTag
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SandooqiThemeTokens

@DrawableRes
fun expensePhotoForGroup(group: String): Int = when (group.trim()) {
    "خضر", "خضر وفواكه" -> R.drawable.img_exp_vegetables
    "فواكه" -> R.drawable.img_exp_fruits
    "لحوم بيضاء" -> R.drawable.img_exp_poultry
    "لحوم حمراء", "لحوم وأسماك" -> R.drawable.img_exp_meat
    "أسماك" -> R.drawable.img_exp_fish
    "التوابل", "توابل" -> R.drawable.img_exp_spices
    else -> R.drawable.img_exp_groceries
}

@DrawableRes
fun expensePhotoForItem(itemName: String, group: String? = null): Int {
    if (!group.isNullOrBlank() && group != "الكل") {
        return expensePhotoForGroup(group)
    }
    val n = itemName.trim()
    return when {
        Regex("كمون|كركم|فلفل أسود|فلفل أكحل|عكري|بابريكا|رأس الحانوت|زنجبيل|قرفة|زعفران|كزبرة|كسبر|هيل|حبهان|قرنفل|جوزة|زعتر|جلجلان|سمسم|رند|غار|يانسون|حبة حلاوة|توابل|بهارات|ثوم بودرة").containsMatchIn(n) -> R.drawable.img_exp_spices
        Regex("بطاطا|بطاطس|طماطم(?! مصبرة)|بصل|ثوم|جزر|قرعة|كوسة|فلفل|باذنجان|خيار|خس|سلاطة|شفلور|بروكلي|ملفوف|كرنب|شمندر|جلبانة|معدنوس|قزبر|نعناع").containsMatchIn(n) -> R.drawable.img_exp_vegetables
        Regex("تفاح|موز|برتقال|فراولة|رمان|ليمون|قارص|مشمش|كرز|تمر|عنب|دلاع|بطيخ").containsMatchIn(n) -> R.drawable.img_exp_fruits
        Regex("دجاج|ديك رومي|داند").containsMatchIn(n) -> R.drawable.img_exp_poultry
        Regex("لحم|بقري|غنمي|هبرة|مرقاز|نقانق").containsMatchIn(n) -> R.drawable.img_exp_meat
        Regex("سردين|سمك|دوراد|سلمون|جمبري|كروفيت|تونة").containsMatchIn(n) -> R.drawable.img_exp_fish
        else -> R.drawable.img_exp_groceries
    }
}

@Composable
fun ExpenseCategoryPhotoStrip(
    groups: List<String>,
    activeGroup: String,
    onSelectGroup: (String) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            val selected = activeGroup == group
            val resId = if (group == "الكل") R.drawable.img_exp_vegetables else expensePhotoForGroup(group)
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) ext.emerald else ext.cardBorder,
                ),
                modifier = Modifier
                    .width(96.dp)
                    .height(82.dp)
                    .clickable { onSelectGroup(group) }
                    .testTag("expense_group_$group"),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = group,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = if (selected) 0.72f else 0.62f),
                                    )
                                )
                            ),
                    )
                    if (selected) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ext.emerald,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(20.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "محدد",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseGroupPhotoBanner(
    activeGroup: String,
    itemCount: Int,
) {
    val resId = if (activeGroup == "الكل") R.drawable.img_exp_groceries else expensePhotoForGroup(activeGroup)
    val subtitle = when (activeGroup) {
        "الكل" -> "$itemCount صنف متاح في جميع الفئات"
        "التوابل" -> "$itemCount نوع من التوابل والعطارة"
        else -> "$itemCount صنف في قسم $activeGroup"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = activeGroup,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.25f),
                            )
                        )
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = if (activeGroup == "الكل") "الكتالوج الغذائي والمشتريات" else activeGroup,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}

@Composable
fun CatalogProductGrid(
    items: List<CatalogItem>,
    selectedItems: List<String>,
    onToggleItem: (CatalogItem) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    items.chunked(2).forEach { pair ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pair.forEach { item ->
                val picked = item.name in selectedItems
                val photoRes = expensePhotoForItem(item.name, item.group)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (picked) ext.emeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (picked) ext.emerald else ext.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleItem(item) }
                        .testTag("catalog_item_${item.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            painter = painterResource(id = photoRes),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (picked) ext.onEmeraldContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.group} · ${item.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (picked) ext.onEmeraldContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (picked) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "محدد",
                                tint = ext.emerald,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpicesTypesSection(
    spiceItems: List<CatalogItem>,
    selectedItems: List<String>,
    onToggleSpice: (String) -> Unit,
    onAddCustomSpice: (String) -> Unit,
    onSendSpicesToMarket: (List<String>) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    var customSpice by remember { mutableStateOf("") }
    val selectedSpices = remember(spiceItems, selectedItems) {
        val spiceNames = spiceItems.map { it.name }.toSet()
        selectedItems.filter { it in spiceNames }
    }

    SandooqiCard {
        SectionHeader(
            title = "خانة أنواع التوابل والعطارة",
            subtitle = "اختر التوابل لإضافتها للمصروف أو لقائمة السوق",
            trailing = {
                BadgeTag("${spiceItems.size} نوع", ext.amberContainer, ext.onAmberContainer)
            },
        )
        Spacer(Modifier.height(8.dp))

        // Real Spices Photo Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_exp_spices),
                    contentDescription = "أنواع التوابل",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.22f),
                                )
                            )
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = "توابل وأعشاب المطبخ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "اضغط على أي نوع لتحديده بالغرام (غ)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            spiceItems.forEach { spice ->
                val picked = spice.name in selectedItems
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (picked) ext.amberContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (picked) ext.amber else ext.cardBorder),
                    modifier = Modifier
                        .clickable { onToggleSpice(spice.name) }
                        .testTag("spice_chip_${spice.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_exp_spices),
                            contentDescription = spice.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                        Text(
                            text = spice.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium,
                            color = if (picked) ext.onAmberContainer else MaterialTheme.colorScheme.onSurface,
                        )
                        if (picked) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "محدد",
                                tint = ext.amber,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        if (selectedSpices.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onSendSpicesToMarket(selectedSpices) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("send_spices_to_market_btn"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "إرسال المحدد (${selectedSpices.size}) لقائمة السوق",
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = customSpice,
                onValueChange = { customSpice = it },
                label = { Text("إضافة نوع توابل آخر...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_spice_input"),
            )
            Button(
                onClick = {
                    val clean = customSpice.trim()
                    if (clean.isNotBlank()) {
                        onAddCustomSpice(clean)
                        customSpice = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ext.amber),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_custom_spice_btn"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("إضافة", fontWeight = FontWeight.Bold)
            }
        }
    }
}
