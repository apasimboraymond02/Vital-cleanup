package com.teraxes.vital.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.ui.theme.RosePrimary
import com.teraxes.vital.ui.theme.RosePrimaryContainer
import com.teraxes.vital.ui.theme.RoseTertiary
import com.teraxes.vital.ui.viewmodel.VitalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FertilityFamilyScreen(
    viewModel: VitalViewModel,
    initialTab: Int = 0
) {
    val activePregnancy by viewModel.activePregnancy.collectAsState()
    var selectedSubTab by remember(activePregnancy) {
        mutableIntStateOf(if (activePregnancy != null) 1 else initialTab)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Pill Tabs for Subsections
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Subsection 1: Fertility & Conception
                    Surface(
                        onClick = { selectedSubTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedSubTab == 0) RosePrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fertility_subtab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = if (selectedSubTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fertility & Ovulation",
                                fontSize = 12.sp,
                                fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedSubTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Subsection 2: Pregnancy Tracker
                    Surface(
                        onClick = { selectedSubTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedSubTab == 1) RosePrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pregnancy_subtab_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChildCare,
                                contentDescription = null,
                                tint = if (selectedSubTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activePregnancy != null) "Pregnancy (Active)" else "Pregnancy Journey",
                                fontSize = 12.sp,
                                fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedSubTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Display Selected Subsection
        Box(modifier = Modifier.weight(1f)) {
            if (selectedSubTab == 0) {
                FertilityScreen(viewModel = viewModel)
            } else {
                PregnancyScreen(viewModel = viewModel)
            }
        }
    }
}
