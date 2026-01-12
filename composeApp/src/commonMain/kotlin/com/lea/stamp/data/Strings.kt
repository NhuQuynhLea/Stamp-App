package com.lea.stamp.data

object Strings {
    
    const val GEMINI_SEGMENTATION_PROMPT = "Give the segmentation masks for the food items. Output a JSON list of segmentation masks where each entry contains the 2D bounding box in the key \"box_2d\", the segmentation mask in key \"mask\", and the text label in the key \"label\". Use descriptive labels"
    
    val GEMINI_ADVICE_PROMPT = """
        You are a supportive and knowledgeable health coach. Your goal is to provide EXTREMELY CONCISE daily advice (maximum 40 words) based on the user's current health metrics and meal usage.
        
        Input Data:
        %1${'$'}s
        
        Rules for Analysis:
        1. **Metric Comparison**: Compare current values to targets.
           - If a metric is significantly OFF (e.g. Calories > 110% of target, or Water < 50% of target), provide a gentle warning.
           - If metrics are on track, offer positive reinforcement.
           
        2. **Meal Completeness & Timing**: Check "Meals Captured" and "Current Time".
           - Ideally, a user should have Breakfast, Lunch, and Dinner.
           - If they missed a meal clearly past its time (e.g. no lunch by 3 PM), ask if they skipped it.
           
        3. **Meal Timing Check**:
           - Breakfast: Should be before 9:00 AM.
           - Lunch: Should be before 1:00 PM.
           - Dinner: Should be before 8:00 PM.
           - If a recent meal was late, gently suggest eating earlier for better digestion/sleep.
           
        Output Format:
        Return ONLY a single paragraph of text (string). NO JSON.
        - Speak directly to the user ("You...", "Try to...").
        - Keep it under 40 words.
        - Combine the most important observations into a natural, encouraging message.
        - If everything is perfect, just say something motivating!
    """.trimIndent()
    
    val GEMINI_MEAL_SUGGESTION_PROMPT = """
        You are a supportive and knowledgeable health coach. Your task is to ONLY suggest the NEXT meal.

        Input Data:
        %1${'$'}s

        Task:
        - Analyze what the user has already eaten and the "Current Time".
        - Suggest a specific, healthy option for the RELEVANT next meal based on time:
           - Morning (04:00 - 10:00): Breakfast
           - mid-day (11:00 - 13:00): Lunch
           - Evening (17:00 - 20:00): Dinner
        - Suggest a specific, healthy option.
        - CRITICAL: Include explicit mention of the KEY METRICS relevant to the user's PRIMARY GOAL (e.g. "Contains ~30g Protein" for Muscle Gain, or "Low calorie (~400kcal)" for Weight Loss).

        Output Format:
        Return a plain text response structured as follows:
        - Start with a short, 1-sentence intro (e.g. "For your [MealType], try this:").
        - Use a bulleted list for the food items or ingredients (start lines with "- ").
        - End with a very short closing comment.
        - Do NOT return JSON. Use \n for line breaks.
    """.trimIndent()

    
    const val GEMINI_BATCH_FOOD_METRICS_PROMPT = """
Analyze the nutritional content of these food items in the image: {{FOOD_LIST}}

Return a JSON object where each KEY is the EXACT food name from the list above, and the value contains its nutritional metrics.

Example format if the list was "rice, chicken, broccoli":
{
  "rice": {
    "calories": 200,
    "protein": 4,
    "fiber": 1,
    "addedSugar": 0,
    "saturatedFat": 0,
    "sodium": 5,
    "vegetableContent": 0,
    "water": 100,
    "processingLevel": 1,
    "isHealthy": true
  },
  "chicken": {
    "calories": 165,
    "protein": 31,
    ...
  },
  "broccoli": {
    ...
  }
}

CRITICAL: Use the EXACT food names from the list "{{FOOD_LIST}}" as the JSON keys. Do NOT use generic names like "Food Item 1" or "Item 1".
Be accurate with nutritional values based on visible portions.
Return ONLY the JSON object, no additional text.
"""
    
    const val GEMINI_FOOD_METRICS_PROMPT = """
Analyze the nutritional content of {{FOOD_NAME}} and provide detailed metrics in the following JSON format:
{
  "calories": <number>,
  "protein": <number in grams>,
  "fiber": <number in grams>,
  "addedSugar": <number in grams>,
  "saturatedFat": <number in grams>,
  "sodium": <number in mg>,
  "vegetableContent": <number in grams>,
  "water": <number in ml>,
  "processingLevel": <number from 1-5, where 1=unprocessed, 5=ultra-processed>,
  "isHealthy": <true or false>
}

Be accurate with the nutritional values for a typical serving of {{FOOD_NAME}}.
Consider it healthy if it has good nutritional balance and low processing level.
Return ONLY the JSON object, no additional text.
"""

    const val GEMINI_DAILY_TARGET_PROMPT = """
        User Profile:
        - Sex: {{SEX}}
        - Age: {{AGE}}
        - Height: {{HEIGHT}} cm
        - Weight: {{WEIGHT}} kg
        - Activity Level: {{ACTIVITY_LEVEL}}
        
        Health Metrics & Context:
        - Primary Goal: {{PRIMARY_GOAL}}
        - Intensity: {{INTENSITY}}
        - Secondary Focus: {{SECONDARY_FOCUS}}
        - Health Conditions: {{HEALTH_CONDITIONS}}
        - Dietary Pattern: {{DIETARY_PATTERN}}
        - Medications: {{MEDICATIONS}}
        
        Task:
        Generate a "Detailed Daily Target" plan in JSON format.
        The JSON must have the following structure:
        {
          "title": "Short catchy title for the plan",
          "description": "A concise, motivating explanation of the strategy (approx 2 sentences).",
          "metrics": [
            {{DYNAMIC_METRICS}}
          ]
        }
        
        Ensure strictly valid JSON. Do not include markdown keys like ```json.
    """
}
