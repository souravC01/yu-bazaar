package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListingPolicy {
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_DESCRIPTION_LENGTH = 255;
    public static final double MAX_PRICE = 100_000.00;

    private static final List<ConditionOption> CONDITIONS = List.of(
            new ConditionOption("new", "Brand New"),
            new ConditionOption("used (like new)", "Used (Like New)"),
            new ConditionOption("used", "Used (Good)"),
            new ConditionOption("poor", "Fair / Poor")
    );

    private static final List<String> LOCATIONS = List.of(
            "Vari Hall",
            "Scott Library",
            "Student Centre",
            "Bergeron Centre for Engineering Excellence",
            "Steacie Science and Engineering Library",
            "York Lanes",
            "Accolade Building East",
            "Accolade Building West",
            "Archives of Ontario",
            "Atkinson",
            "Bennett Centre for Student Services",
            "Behavioural Sciences Building",
            "Burton Auditorium",
            "Calumet College",
            "Chemistry Building",
            "Computer Methods Building",
            "Central Square",
            "Central Utilities Building",
            "Centre for Film and Theatre",
            "Curtis Lecture Halls",
            "Dahdaleh Building",
            "Executive Learning Centre",
            "Farquharson Life Sciences",
            "Founders College",
            "Founders Tennis Court",
            "Frost Library (Glendon campus)",
            "Glendon Hall (Glendon campus)",
            "Hart House (Osgoode Hall Law School)",
            "Health, Nursing and Environmental Studies Building",
            "Hilliard Residence (Glendon campus)",
            "Ignat Kaneff Building",
            "Kaneff Tower",
            "Kinsmen Building",
            "Lassonde Building",
            "LA&PS @ IBM Markham",
            "Life Sciences Building",
            "Lorna R. Marsden Honours Court & Welcome Centre",
            "Lumbers Building",
            "McLaughlin College",
            "Norman Bethune College",
            "Petrie Science and Engineering Building",
            "Physical Resources Building",
            "Rob and Cheryl McEwen Graduate Study Building",
            "Ross Building - North wing",
            "Ross Building - South wing",
            "Seneca @ York",
            "Seymour Schulich Building",
            "Sheridan College - Trafalgar Campus",
            "Sherman Health Science Research Centre",
            "Stedman Lecture Halls",
            "Stong College",
            "Tait McKenzie Centre",
            "Tait Tennis Courts",
            "Technology and Enhanced Learning Building",
            "Tennis Canada",
            "The Joan & Martin Goldfarb Centre for Fine Arts",
            "Track and Field Centre",
            "Vanier College",
            "West Office Building",
            "William Small Centre",
            "Winters College",
            "York Hall (Glendon campus)",
            "Off Campus"
    );

    public List<ConditionOption> conditions() {
        return CONDITIONS;
    }

    public List<String> locations() {
        return LOCATIONS;
    }

    public ValidationResult validate(String title, double price, String condition, String location, String description) {
        if (title == null || title.isBlank()) {
            return ValidationResult.invalid("Enter a title.");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            return ValidationResult.invalid("Title must be 120 characters or fewer.");
        }
        if (!Double.isFinite(price) || price < 0 || price > MAX_PRICE) {
            return ValidationResult.invalid("Price must be between $0.00 and $100,000.00.");
        }
        if (CONDITIONS.stream().noneMatch(option -> option.value().equals(condition))) {
            return ValidationResult.invalid("Invalid condition selected.");
        }
        if (!LOCATIONS.contains(location)) {
            return ValidationResult.invalid("Invalid location selected.");
        }
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            return ValidationResult.invalid("Description must be 255 characters or fewer.");
        }
        return ValidationResult.success();
    }

    public record ConditionOption(String value, String label) {
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}
