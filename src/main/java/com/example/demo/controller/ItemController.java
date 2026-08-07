package com.example.demo.controller;

import com.example.demo.Email.EmailSender;
import com.example.demo.Email.EmailTemplate;
import com.example.demo.model.Item;
import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class ItemController {
    private static final Set<String> VALID_WEAR_OPTIONS = Set.of("new", "used (like new)", "used", "poor");
    private static final List<String> VALID_LOCATIONS = Arrays.asList(
            "Accolade Building East", "Accolade Building West", "Archives of Ontario", "Atkinson",
            "Norman Bethune College", "Bennett Centre for Student Services", "Bergeron Centre for Engineering Excellence",
            "Behavioural Sciences Building", "Burton Auditorium", "Chemistry Building", "Calumet College",
            "The Joan & Martin Goldfarb Centre for Fine Arts", "Centre for Film and Theatre", "Curtis Lecture Halls",
            "Computer Methods Building", "Central Square", "Central Utilities Building", "Dahdaleh Building",
            "Executive Learning Centre", "Founders College", "Frost Library (Glendon campus)",
            "Farquharson Life Sciences", "Founders Tennis Court", "Glendon Hall (Glendon campus)",
            "Lorna R. Marsden Honours Court & Welcome Centre", "Hart House (Osgoode Hall Law School)",
            "Health, Nursing and Environmental Studies Building", "Hilliard Residence (Glendon campus)",
            "Ignat Kaneff Building", "Kinsmen Building", "Kaneff Tower", "Lassonde Building", "LA&PS @ IBM Markham",
            "Life Sciences Building", "Lumbers Building", "Rob and Cheryl McEwen Graduate Study & Research Building",
            "McLaughlin College", "Off Campus", "Physical Resources Building",
            "Petrie Science and Engineering Building / Petrie Observatory", "Ross Building - North wing",
            "Ross Building - South wing", "Seneca @ York", "Stong College", "Scott Library",
            "Sherman Health Science Research Centre", "Stedman Lecture Halls", "Seymour Schulich Building",
            "Sheridan College - Trafalgar Campus", "Student Centre", "Steacie Science and Engineering Library",
            "Tennis Canada", "Technology and Enhanced Learning Building", "Track and Field Centre",
            "Tait McKenzie Centre", "Tait Tennis Courts", "Vanier College", "Vari Hall", "Winters College",
            "West Office Building", "William Small Centre", "York Hall (Glendon campus)", "York Lanes"
    );
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final Path uploadDirectory;

    public ItemController(ItemRepository itemRepository,
                          UserRepository userRepository,
                          EmailSender emailSender,
                          @Value("${app.upload.directory}") String uploadDirectory) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @GetMapping("/home")
    public String viewHomePage(Model model) {
        model.addAttribute("items", itemRepository.findAll());
        return "home_page";
    }

    @GetMapping("/product/{id}")
    public String viewProductDetails(@PathVariable Long id, Authentication authentication, Model model) {
        Item item = getItem(id);
        model.addAttribute("item", item);
        model.addAttribute("isOwner", ownsItem(item, authentication));
        return "product_page";
    }

    @PostMapping("/add-item")
    public String addItem(@RequestParam String title,
                          @RequestParam double price,
                          @RequestParam String wear,
                          @RequestParam String location,
                          @RequestParam String description,
                          @RequestParam("image") MultipartFile imageFile,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (title.isBlank() || price < 0) {
            return homeWithError(model, "Enter a title and a non-negative price.");
        }
        if (!VALID_WEAR_OPTIONS.contains(wear)) {
            return homeWithError(model, "Invalid wear condition selected.");
        }
        if (!VALID_LOCATIONS.contains(location)) {
            return homeWithError(model, "Invalid location selected.");
        }

        Path storedImage = null;
        try {
            storedImage = storeImage(imageFile);

            Item item = new Item();
            item.setTitle(title.trim());
            item.setPrice(price);
            item.setWear(wear);
            item.setLocation(location);
            item.setDescription(description.trim());
            item.setSellerEmail(authentication.getName());
            item.setImagePath(storedImage.getFileName().toString());
            itemRepository.save(item);

            EmailTemplate template = EmailTemplate.LISTING_CONFIRMATION;
            emailSender.sendEmail(authentication.getName(), template.getSubject(), template.getBody(item.getTitle()));

            redirectAttributes.addFlashAttribute("success", "Item added successfully.");
            return "redirect:/home";
        } catch (IllegalArgumentException exception) {
            return homeWithError(model, exception.getMessage());
        } catch (Exception exception) {
            deleteQuietly(storedImage);
            return homeWithError(model, "The item could not be added. Please try again.");
        }
    }

    @PostMapping("/send-inquiry")
    public String sendInquiry(@RequestParam Long itemId,
                              @RequestParam String message,
                              Authentication authentication,
                              Model model) {
        Item item = getItem(itemId);
        User buyer = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        String subject = "New Inquiry for Your Listing on YU Bazaar";
        String emailBody = String.format(
                "Hi,\n\nYou have received a new inquiry for your listing titled '%s'.\n\n" +
                        "Inquiry Details:\nBuyer Name: %s\nBuyer Email: %s\nMessage: %s\n\n" +
                        "You can contact the buyer directly to follow up.\n\nRegards,\nYU Bazaar Team",
                item.getTitle(), buyer.getName(), buyer.getEmail(), message.trim()
        );

        boolean delivered = emailSender.sendEmail(item.getSellerEmail(), subject, emailBody);
        model.addAttribute("success", delivered
                ? "Inquiry sent successfully to the seller."
                : "Email delivery is disabled in this local environment.");
        model.addAttribute("item", item);
        model.addAttribute("isOwner", ownsItem(item, authentication));
        return "product_page";
    }

    @PostMapping("/delete-item")
    public String deleteItem(@RequestParam Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        Item item = getItem(id);
        if (!ownsItem(item, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        itemRepository.delete(item);
        deleteQuietly(uploadDirectory.resolve(item.getImagePath()).normalize());
        redirectAttributes.addFlashAttribute("success", "Listing deleted.");
        return "redirect:/home";
    }

    @GetMapping("/search")
    public String searchItems(@RequestParam String keyword, Model model) {
        List<Item> searchResults = itemRepository.searchItems(keyword.trim());
        model.addAttribute("items", searchResults);
        model.addAttribute("searchKeyword", keyword.trim());
        return "home_page";
    }

    @GetMapping("/search-suggestions")
    @ResponseBody
    public List<String> getSearchSuggestions(@RequestParam String keyword) {
        return itemRepository.searchItems(keyword.trim())
                .stream()
                .map(Item::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .distinct()
                .limit(8)
                .toList();
    }

    private Item getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean ownsItem(Item item, Authentication authentication) {
        return item.getSellerEmail() != null
                && item.getSellerEmail().equalsIgnoreCase(authentication.getName());
    }

    private String homeWithError(Model model, String error) {
        model.addAttribute("items", itemRepository.findAll());
        model.addAttribute("error", error);
        return "home_page";
    }

    private Path storeImage(MultipartFile imageFile) throws IOException {
        if (imageFile.isEmpty()) {
            throw new IllegalArgumentException("Select an image for the listing.");
        }

        String contentType = imageFile.getContentType() == null
                ? ""
                : imageFile.getContentType().toLowerCase(Locale.ROOT);
        String extension = IMAGE_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Upload a JPG, PNG, GIF, or WebP image.");
        }

        Files.createDirectories(uploadDirectory);
        Path destination = uploadDirectory.resolve(UUID.randomUUID() + extension).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid image filename.");
        }
        try (InputStream inputStream = imageFile.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination;
    }

    private void deleteQuietly(Path path) {
        if (path == null || !path.startsWith(uploadDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A stale image is preferable to failing the user's database action.
        }
    }
}
