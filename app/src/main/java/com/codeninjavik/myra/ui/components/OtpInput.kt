package com.codeninjavik.myra.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeninjavik.myra.ui.theme.Hairline
import com.codeninjavik.myra.ui.theme.Lifted
import com.codeninjavik.myra.ui.theme.RedAccent
import com.codeninjavik.myra.ui.theme.TextPrimary

@Composable
fun OtpInput(
    otpLength: Int,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember { List(otpLength) { FocusRequester() } }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0 until otpLength) {
            val char = value.getOrNull(i)?.toString() ?: ""

            OutlinedTextField(
                value = char,
                onValueChange = { newVal ->
                    if (newVal.length <= 1) {
                        val updatedValue = buildString {
                            for (j in 0 until otpLength) {
                                if (j == i) {
                                    append(newVal)
                                } else {
                                    append(value.getOrNull(j) ?: "")
                                }
                            }
                        }
                        onValueChange(updatedValue.take(otpLength))
                        
                        if (newVal.isNotEmpty() && i < otpLength - 1) {
                            focusRequesters[i + 1].requestFocus()
                        }
                    }
                },
                modifier = Modifier
                    .width(50.dp)
                    .height(60.dp)
                    .focusRequester(focusRequesters[i])
                    .border(1.dp, if (char.isNotEmpty()) RedAccent else Hairline, RoundedCornerShape(8.dp)),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedAccent,
                    unfocusedBorderColor = Hairline,
                    focusedContainerColor = Lifted,
                    unfocusedContainerColor = Lifted
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters.firstOrNull()?.requestFocus()
    }
}
