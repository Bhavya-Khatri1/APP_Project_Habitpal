import java.util.Random;

public class MotivationQuotes {
    private static final String[] quotes = {
        "Small steps every day lead to big changes.",
        "Discipline beats motivation.",
        "Success doesn’t come from what you do occasionally, it comes from what you do consistently.",
        "The secret of your future is hidden in your daily routine.",
        "You don’t have to be extreme, just consistent.",
        "Push yourself, no one else is going to do it for you.",
        "The man who moves a mountain begins by carrying small stones.",
        "Motivation gets you started, habit keeps you going.",
        "Consistency is the bridge between goals and success.",
        "If you get tired, learn to rest, not to quit.",
        "The future depends on what you do today.",
        "It always seems impossible until it’s done.",
        "Don’t wish for it, work for it.",
        "You don’t need to be perfect, you just need to start.",
        "Good habits formed at youth make all the difference.",
        "Either you run the day or the day runs you.",
        "Your daily routine determines your destiny.",
        "Focus on progress, not perfection.",
        "Winners are not those who never fail, but those who never quit.",
        "Make every day count toward your goal.",
        "Motivation is what gets you started. Habit is what keeps you going.",
        "You’ll never always be motivated, so you must learn to be disciplined.",
        "The best way to predict your future is to create it.",
        "Success is nothing more than a few simple disciplines practiced every day.",
        "Don’t count the days, make the days count.",
        "Start where you are. Use what you have. Do what you can.",
        "Each day is another chance to improve yourself.",
        "Dream big. Start small. Act now.",
        "You don’t find willpower, you create it.",
        "Make habits your superpower."
    };

    public static String getRandomQuote() {
        Random rand = new Random();
        return quotes[rand.nextInt(quotes.length)];
    }
}