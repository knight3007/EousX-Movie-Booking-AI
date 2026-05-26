package com.uit.eousx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val inputShape = RoundedCornerShape(EousXRadius.md)

private val inputColors
    @Composable get() = TextFieldDefaults.colors(
        focusedContainerColor = EousXColors.SurfaceVariant,
        unfocusedContainerColor = EousXColors.SurfaceVariant,
        focusedIndicatorColor = EousXColors.Transparent,
        unfocusedIndicatorColor = EousXColors.Transparent,
        cursorColor = EousXColors.PrimaryOrange,
        focusedTextColor = EousXColors.SoftIvory,
        unfocusedTextColor = EousXColors.SoftIvory,
        focusedPlaceholderColor = EousXColors.SlateGray,
        unfocusedPlaceholderColor = EousXColors.SlateGray,
        focusedLeadingIconColor = EousXColors.SlateGray,
        unfocusedLeadingIconColor = EousXColors.SlateGray,
        focusedTrailingIconColor = EousXColors.SlateGray,
        unfocusedTrailingIconColor = EousXColors.SlateGray,
    )

@Composable
fun EousXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector = Icons.Default.Person,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val borderColor = when {
        isError -> EousXColors.DangerRed
        value.isNotEmpty() -> EousXColors.PrimaryOrange.copy(alpha = 0.6f)
        else -> EousXColors.Transparent
    }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(inputShape)
            .border(1.dp, borderColor, inputShape),
        enabled = enabled,
        singleLine = singleLine,
        placeholder = {
            Text(placeholder, style = EousXTypography.Body)
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = trailingIcon,
        textStyle = EousXTypography.Body.copy(color = EousXColors.SoftIvory),
        colors = inputColors,
        shape = inputShape,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
fun EousXPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Password",
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(inputShape),
        enabled = enabled,
        singleLine = true,
        placeholder = {
            Text(placeholder, style = EousXTypography.Body)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        textStyle = EousXTypography.Body.copy(color = EousXColors.SoftIvory),
        colors = inputColors,
        shape = inputShape,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
fun EousXSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search movies, actors...",
    onFilterClick: (() -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(inputShape),
        singleLine = true,
        placeholder = {
            Text(placeholder, style = EousXTypography.Body)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (onFilterClick != null) {
            {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            null
        },
        textStyle = EousXTypography.Body.copy(color = EousXColors.SoftIvory),
        colors = inputColors,
        shape = inputShape,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Default
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch?.invoke(value) }
        )
    )
}

@Preview(name = "EousX Text Fields", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXTextFieldsPreview() {
    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
        ) {
            EousXTextField(
                value = "Nguyen An",
                onValueChange = {},
                placeholder = "Enter your name"
            )
            EousXPasswordField(value = "secret", onValueChange = {})
            EousXSearchBar(
                value = "Dune",
                onValueChange = {},
                onFilterClick = {}
            )
        }
    }
}
