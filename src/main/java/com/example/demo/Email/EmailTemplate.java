package com.example.demo.Email;


public enum EmailTemplate {
    REGISTRATION_SUCCESS(
            "Welcome to Yu Bazaar – Registration Successful!",
            "Hi %s,\n\n" +
                    "Welcome to YU Bazaar, a marketplace for York community members and public sellers.\n\n" +
                    "Your registration was received. Please verify your email before signing in.\n\n" +
                    "At Yu Bazaar, you can:\n" +
                    "- List items you want to sell or trade.\n" +
                    "- Browse items from York Verified Students and Public Sellers.\n" +
                    "- Arrange convenient exchanges with other marketplace members.\n\n" +
                    "We’re excited to have you join our growing marketplace!\n" +
                    "If you have any questions, feel free to reach out to us at yubazaarsupport@gmail.com.\n\n" +
                    "Let’s make buying and selling on campus easy, fun, and secure.\n\n" +
                    "Happy shopping and selling!\n" +
                    "The Yu Bazaar Team\n\n" +
                    "Support: yubazaarsupport@gmail.com"
    ),
    PASSWORD_RESET(
            "Reset Your YU Bazaar Password",
            "Hi %s,\n\n" +
                    "Use the link below to reset your YU Bazaar password. This link expires in 30 minutes and can be used once.\n\n" +
                    "%s\n\n" +
                    "If you did not request this change, you can ignore this email.\n\n" +
                    "The YU Bazaar Team"
    ),
    LISTING_CONFIRMATION("Your Listing is Live on YU Bazaar!",
            "Hi,\n\n" +
            "Your listing titled '%s' has been successfully posted on YU Bazaar.\n\n" +
            "Thank you for using YU Bazaar! You can now manage your listing from your account dashboard.\n\n" +
            "Happy selling!\n\n" +
			"The Yu Bazaar Team");


    private final String subject;
    private final String body;

    EmailTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }
// Getters
    public String getSubject() {
        return subject;
    }

    public String getBody(String... params) {
        return String.format(body, (Object[]) params);
    }
}
