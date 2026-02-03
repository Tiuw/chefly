package com.skripsi.chefly.data

object RecipeRepository {
    private val allRecipes = listOf(
        Recipe(
            id = 1,
            name = "Pasta Tomat Klasik",
            description = "A simple and delicious pasta dish with fresh tomatoes and basil",
            imageUrl = "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=800",
            ingredients = listOf(
                "Pasta",
                "Tomat",
                "Bawang Putih",
                "Olive Oil",
                "Basil",
                "Garam",
                "Merica"
            ),
            instructions = listOf(
                "Boil water and cook pasta according to package instructions",
                "Heat olive oil in a pan and sauté minced garlic",
                "Add diced tomatoes and cook for 10 minutes",
                "Season with salt and pepper",
                "Toss cooked pasta with the sauce",
                "Garnish with fresh basil and serve"
            ),
            prepTime = "10 min",
            cookTime = "20 min",
            servings = 4,
            difficulty = "Easy",
            category = "Italian"
        ),
        Recipe(
            id = 2,
            name = "Tumis Ayam Sayuran",
            description = "Quick and healthy chicken stir fry with vegetables",
            imageUrl = "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=800",
            ingredients = listOf(
                "Ayam",
                "Brokoli",
                "Wortel",
                "Cabai Hijau",
                "Kecap",
                "Bawang Putih",
                "Jahe",
                "Nasi"
            ),
            instructions = listOf(
                "Cut chicken into bite-sized pieces",
                "Chop all vegetables",
                "Heat oil in a wok or large pan",
                "Cook chicken until golden brown, set aside",
                "Stir fry vegetables for 3-4 minutes",
                "Add chicken back to the pan",
                "Add soy sauce, garlic, and ginger",
                "Serve over steamed rice"
            ),
            prepTime = "15 min",
            cookTime = "15 min",
            servings = 4,
            difficulty = "Easy",
            category = "Asian"
        ),
        Recipe(
            id = 3,
            name = "Salad Sayuran Segar",
            description = "Fresh and healthy mixed vegetable salad",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800",
            ingredients = listOf(
                "Lettuce",
                "Tomat",
                "Timun",
                "Wortel",
                "Cabai Hijau",
                "Olive Oil",
                "Lemon",
                "Garam"
            ),
            instructions = listOf(
                "Wash all vegetables thoroughly",
                "Chop lettuce, tomatoes, cucumber, and bell pepper",
                "Grate or julienne carrot",
                "Mix all vegetables in a large bowl",
                "Drizzle with olive oil and lemon juice",
                "Season with salt and toss well",
                "Serve immediately"
            ),
            prepTime = "15 min",
            cookTime = "0 min",
            servings = 2,
            difficulty = "Easy",
            category = "Salad"
        ),
        Recipe(
            id = 4,
            name = "Smoothie Pisang",
            description = "Creamy and nutritious banana smoothie",
            imageUrl = "https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=800",
            ingredients = listOf("Pisang", "Susu", "Madu", "Es"),
            instructions = listOf(
                "Peel and slice bananas",
                "Add bananas, milk, honey, and ice to blender",
                "Blend until smooth and creamy",
                "Pour into glasses and serve immediately"
            ),
            prepTime = "5 min",
            cookTime = "0 min",
            servings = 2,
            difficulty = "Easy",
            category = "Beverage"
        ),
        Recipe(
            id = 5,
            name = "Nasi Goreng Telur",
            description = "Simple and tasty fried rice with eggs",
            imageUrl = "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=800",
            ingredients = listOf(
                "Nasi",
                "Telur",
                "Wortel",
                "Kacang Polong",
                "Kecap",
                "Bawang Putih",
                "Minyak"
            ),
            instructions = listOf(
                "Cook rice and let it cool (preferably day-old rice)",
                "Beat eggs and scramble in a pan, set aside",
                "Heat oil and sauté minced garlic",
                "Add diced carrots and peas, cook for 2 minutes",
                "Add rice and stir fry for 3-4 minutes",
                "Add scrambled eggs and soy sauce",
                "Mix well and serve hot"
            ),
            prepTime = "10 min",
            cookTime = "15 min",
            servings = 3,
            difficulty = "Easy",
            category = "Asian"
        ),
        Recipe(
            id = 6,
            name = "Pai Apel",
            description = "Classic homemade apple pie with flaky crust",
            imageUrl = "https://images.unsplash.com/photo-1535920527002-b35e96722eb9?w=800",
            ingredients = listOf("Apel", "Tepung", "Mentega", "Gula", "Kayu Manis", "Telur"),
            instructions = listOf(
                "Prepare pie crust with flour, butter, and cold water",
                "Peel and slice apples",
                "Mix apples with sugar and cinnamon",
                "Roll out pie crust and place in pie dish",
                "Fill with apple mixture",
                "Cover with top crust and seal edges",
                "Brush with beaten egg",
                "Bake at 375°F for 45-50 minutes"
            ),
            prepTime = "30 min",
            cookTime = "50 min",
            servings = 8,
            difficulty = "Medium",
            category = "Dessert"
        ),
        Recipe(
            id = 7,
            name = "Sandwich Keju Panggang",
            description = "Perfect crispy grilled cheese sandwich",
            imageUrl = "https://images.unsplash.com/photo-1528736235302-52922df5c122?w=800",
            ingredients = listOf("Roti", "Keju", "Mentega"),
            instructions = listOf(
                "Butter one side of each bread slice",
                "Place cheese between bread slices, butter side out",
                "Heat a pan over medium heat",
                "Grill sandwich until golden brown on both sides",
                "Cheese should be melted and gooey",
                "Cut in half and serve hot"
            ),
            prepTime = "5 min",
            cookTime = "10 min",
            servings = 1,
            difficulty = "Easy",
            category = "Sandwich"
        ),
        Recipe(
            id = 8,
            name = "Kue Coklat",
            description = "Rich and moist chocolate cake",
            imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=800",
            ingredients = listOf(
                "Tepung",
                "Bubuk Coklat",
                "Gula",
                "Telur",
                "Mentega",
                "Susu",
                "Baking Powder"
            ),
            instructions = listOf(
                "Preheat oven to 350°F",
                "Mix flour, cocoa powder, sugar, and baking powder",
                "In another bowl, beat eggs with melted butter and milk",
                "Combine wet and dry ingredients",
                "Pour batter into greased cake pan",
                "Bake for 30-35 minutes",
                "Let cool before frosting"
            ),
            prepTime = "20 min",
            cookTime = "35 min",
            servings = 12,
            difficulty = "Medium",
            category = "Dessert"
        ),
        Recipe(
            id = 9,
            name = "Salad Caesar",
            description = "Classic Caesar salad with crispy croutons",
            imageUrl = "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=800",
            ingredients = listOf(
                "Selada",
                "Roti",
                "Keju",
                "Bawang Putih",
                "Telur",
                "Lemon",
                "Olive Oil"
            ),
            instructions = listOf(
                "Wash and chop romaine lettuce",
                "Make croutons by toasting bread cubes with garlic and oil",
                "Prepare Caesar dressing with egg yolk, lemon, garlic, and oil",
                "Toss lettuce with dressing",
                "Top with croutons and grated cheese",
                "Serve immediately"
            ),
            prepTime = "15 min",
            cookTime = "10 min",
            servings = 4,
            difficulty = "Medium",
            category = "Salad"
        ),
        Recipe(
            id = 10,
            name = "Sup Kentang",
            description = "Creamy and comforting potato soup",
            imageUrl = "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=800",
            ingredients = listOf(
                "Kentang",
                "Bawang Merah",
                "Bawang Putih",
                "Susu",
                "Mentega",
                "Garam",
                "Merica"
            ),
            instructions = listOf(
                "Peel and dice potatoes",
                "Sauté chopped onion and garlic in butter",
                "Add potatoes and vegetable broth",
                "Simmer until potatoes are tender",
                "Blend half of the soup for creamy texture",
                "Add milk and season with salt and pepper",
                "Serve hot with garnish"
            ),
            prepTime = "15 min",
            cookTime = "30 min",
            servings = 6,
            difficulty = "Easy",
            category = "Soup"
        ),
        // Additional Indonesian recipes using detected ingredients
        Recipe(
            id = 11,
            name = "Osso Buco Ayam",
            description = "Tender chicken braised with vegetables",
            imageUrl = "https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=800",
            ingredients = listOf(
                "Ayam",
                "Tomat",
                "Wortel",
                "Bawang Merah",
                "Bawang Putih",
                "Daun Bawang"
            ),
            instructions = listOf(
                "Season chicken pieces with salt and pepper",
                "Brown chicken in a large pot",
                "Remove chicken and sauté onions, garlic, and carrots",
                "Add tomatoes and bring to simmer",
                "Return chicken to pot, cover and braise for 1 hour",
                "Garnish with green onions and serve"
            ),
            prepTime = "20 min",
            cookTime = "60 min",
            servings = 6,
            difficulty = "Medium",
            category = "Asian"
        ),
        Recipe(
            id = 12,
            name = "Tumis Tahu Tempe",
            description = "Stir-fried tofu and tempeh with spices",
            imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800",
            ingredients = listOf(
                "Tahu",
                "Tempe",
                "Bawang Merah",
                "Bawang Putih",
                "Cabai Merah",
                "Kecap Manis"
            ),
            instructions = listOf(
                "Cut tofu and tempeh into cubes",
                "Fry tofu and tempeh until golden, set aside",
                "Sauté sliced onions, garlic, and chilies",
                "Add fried tofu and tempeh back to pan",
                "Add sweet soy sauce and stir well",
                "Cook for 2-3 minutes and serve hot"
            ),
            prepTime = "10 min",
            cookTime = "15 min",
            servings = 4,
            difficulty = "Easy",
            category = "Asian"
        ),
        Recipe(
            id = 13,
            name = "Sayur Lodeh",
            description = "Mixed vegetables in coconut milk soup",
            imageUrl = "https://images.unsplash.com/photo-1547592180-85f173990554?w=800",
            ingredients = listOf(
                "Kol",
                "Wortel",
                "Kacang Panjang",
                "Tahu",
                "Tempe",
                "Santan",
                "Bawang Merah",
                "Bawang Putih"
            ),
            instructions = listOf(
                "Sauté ground spices (onions, garlic) until fragrant",
                "Add coconut milk and bring to gentle simmer",
                "Add harder vegetables first (carrots, cabbage)",
                "Add tofu, tempeh, and long beans",
                "Season with salt and sugar",
                "Simmer until vegetables are tender",
                "Serve with rice"
            ),
            prepTime = "15 min",
            cookTime = "25 min",
            servings = 6,
            difficulty = "Easy",
            category = "Asian"
        ),
        Recipe(
            id = 14,
            name = "Capcay",
            description = "Mixed vegetable stir-fry in savory sauce",
            imageUrl = "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800",
            ingredients = listOf(
                "Kol",
                "Wortel",
                "Bawang Putih",
                "Bawang Merah",
                "Cabai Hijau",
                "Ayam",
                "Udang"
            ),
            instructions = listOf(
                "Cut all vegetables into bite-sized pieces",
                "Heat oil and stir-fry garlic and onions",
                "Add chicken and shrimp, cook until done",
                "Add harder vegetables (carrots) first",
                "Add cabbage and peppers",
                "Add oyster sauce and cornstarch slurry",
                "Stir until sauce thickens, serve hot"
            ),
            prepTime = "20 min",
            cookTime = "15 min",
            servings = 4,
            difficulty = "Medium",
            category = "Asian"
        ),
        Recipe(
            id = 15,
            name = "Telur Dadar Sayur",
            description = "Vegetable omelet Indonesian style",
            imageUrl = "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800",
            ingredients = listOf(
                "Telur",
                "Tomat",
                "Bawang Merah",
                "Daun Bawang",
                "Cabai Merah",
                "Garam"
            ),
            instructions = listOf(
                "Beat eggs in a bowl",
                "Add diced tomatoes, sliced onions, green onions, and chilies",
                "Season with salt and pepper",
                "Heat oil in a pan",
                "Pour egg mixture and cook until set on bottom",
                "Flip and cook other side until golden",
                "Serve hot with rice"
            ),
            prepTime = "10 min",
            cookTime = "10 min",
            servings = 2,
            difficulty = "Easy",
            category = "Asian"
        )
    )

    fun getAllRecipes(): List<Recipe> = allRecipes

    fun getRecipeById(id: Int): Recipe? = allRecipes.find { it.id == id }

    fun searchRecipesByIngredients(detectedIngredients: List<String>): List<Recipe> {
        if (detectedIngredients.isEmpty()) return emptyList()

        // Normalize detected ingredients for comparison (lowercase and trim)
        val normalizedDetected = detectedIngredients.map { it.lowercase().trim() }

        return allRecipes.filter { recipe ->
            // Check if recipe contains any of the detected ingredients
            recipe.ingredients.any { ingredient ->
                normalizedDetected.any { detected ->
                    ingredient.lowercase().trim().contains(detected) ||
                    detected.contains(ingredient.lowercase().trim())
                }
            }
        }.sortedByDescending { recipe ->
            // Sort by number of matching ingredients (most matches first)
            recipe.ingredients.count { ingredient ->
                normalizedDetected.any { detected ->
                    ingredient.lowercase().trim().contains(detected) ||
                    detected.contains(ingredient.lowercase().trim())
                }
            }
        }
    }
}
