package com.example.cmiyc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.cmiyc.data.Friend

/**
 * A reusable search bar component with an icon and customizable placeholder.
 *
 * This component provides a standard search input field with Material Design styling.
 * It includes a search icon and supports a customizable placeholder text.
 *
 * @param query The current search query string.
 * @param onQueryChange Callback invoked when the search query changes.
 * @param placeholder Text displayed when the search field is empty, defaults to "Search by name or email".
 * @param modifier Optional modifier for customizing the component layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search by name or email",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        singleLine = true
    )
}

/**
 * A composable that displays information about a friend with removal capability.
 *
 * This card-based component shows a friend's name, email, and a delete button
 * to remove the friendship. It uses Material Design styling with appropriate spacing
 * and elevation.
 *
 * @param friend The Friend data object containing details to display.
 * @param onRemoveFriend Callback invoked when the remove friend button is clicked.
 * @param modifier Optional modifier for customizing the component layout.
 */
@Composable
fun FriendItem(
    friend: Friend,
    onRemoveFriend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Column {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = friend.email,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            IconButton(onClick = onRemoveFriend) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove Friend",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("removeFriend_button")
                )
            }
        }
    }
}