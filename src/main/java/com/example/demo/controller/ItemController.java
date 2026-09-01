package com.example.demo.controller;

import com.example.demo.Email.EmailSender;
import com.example.demo.Email.EmailTemplate;
import com.example.demo.model.Item;
import com.example.demo.model.User;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.DemoAccountPolicy;
import com.example.demo.service.ListingPolicy;
import com.example.demo.storage.ImageStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

import java.util.List;
import java.util.UUID;

@Controller
public class ItemController {
    private static final String LISTING_SUBMISSION_TOKEN = "listingSubmissionToken";
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final ImageStorageService imageStorageService;
    private final DemoAccountPolicy demoAccountPolicy;
    private final ListingPolicy listingPolicy;

    public ItemController(ItemRepository itemRepository,
                          UserRepository userRepository,
                          EmailSender emailSender,
                          ImageStorageService imageStorageService,
                          DemoAccountPolicy demoAccountPolicy,
                          ListingPolicy listingPolicy) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.imageStorageService = imageStorageService;
        this.demoAccountPolicy = demoAccountPolicy;
        this.listingPolicy = listingPolicy;
    }

    @GetMapping({"/", "/home"})
    public String viewHomePage(@RequestParam(required = false, defaultValue = "false") boolean yorkOnly,
                               Model model,
                               HttpSession session,
                               Authentication authentication) {
        populateUserContext(model, authentication);
        prepareHomeModel(model, session, yorkOnly);
        return "home_page";
    }

    @GetMapping("/product/{id}")
    public String viewProductDetails(@PathVariable Long id, Authentication authentication, Model model) {
        populateUserContext(model, authentication);
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
                          @RequestParam(required = false) String submissionToken,
                          Authentication authentication,
                          HttpSession session,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        demoAccountPolicy.requireWritable(authentication);
        if (!consumeListingSubmissionToken(session, submissionToken)) {
            redirectAttributes.addFlashAttribute("error", "This listing was already submitted. Refresh the page to try again.");
            return "redirect:/home";
        }
        ListingPolicy.ValidationResult validation = listingPolicy.validate(title, price, wear, location, description);
        if (!validation.valid()) {
            return homeWithError(model, session, validation.message(), authentication);
        }

        String storedImageKey = null;
        try {
            storedImageKey = imageStorageService.store(imageFile);

            Item item = new Item();
            item.setTitle(title.trim());
            item.setPrice(price);
            item.setWear(wear);
            item.setLocation(location);
            item.setDescription(description.trim());
            item.setSellerEmail(authentication.getName());
            item.setImagePath(storedImageKey);
            itemRepository.save(item);

            EmailTemplate template = EmailTemplate.LISTING_CONFIRMATION;
            emailSender.sendEmail(authentication.getName(), template.getSubject(), template.getBody(item.getTitle()));

            redirectAttributes.addFlashAttribute("success", "Item added successfully.");
            return "redirect:/home";
        } catch (IllegalArgumentException exception) {
            return homeWithError(model, session, exception.getMessage(), authentication);
        } catch (Exception exception) {
            imageStorageService.deleteQuietly(storedImageKey);
            return homeWithError(model, session, "The item could not be added. Please try again.", authentication);
        }
    }

    @PostMapping("/send-inquiry")
    public String sendInquiry(@RequestParam Long itemId,
                              @RequestParam String message,
                              Authentication authentication,
                              Model model) {
        demoAccountPolicy.requireWritable(authentication);
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
        demoAccountPolicy.requireWritable(authentication);
        Item item = getItem(id);
        if (!ownsItem(item, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        itemRepository.delete(item);
        imageStorageService.deleteQuietly(item.getImagePath());
        redirectAttributes.addFlashAttribute("success", "Listing deleted.");
        return "redirect:/home";
    }

    @GetMapping("/search")
    public String searchItems(@RequestParam(required = false, defaultValue = "") String keyword,
                              @RequestParam(required = false, defaultValue = "false") boolean yorkOnly,
                              Model model,
                              HttpSession session,
                              Authentication authentication) {
        populateUserContext(model, authentication);
        List<Item> searchResults = keyword.isBlank() ? itemRepository.findAll() : itemRepository.searchItems(keyword.trim());
        if (yorkOnly) {
            searchResults = searchResults.stream().filter(Item::isSellerYorkVerified).toList();
        }
        model.addAttribute("items", searchResults);
        model.addAttribute("searchKeyword", keyword.trim());
        model.addAttribute("yorkOnly", yorkOnly);
        addListingOptions(model);
        issueListingSubmissionToken(model, session);
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

    private void populateUserContext(Model model, Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("isAuthenticated", authenticated);
        if (authenticated) {
            userRepository.findByEmailIgnoreCase(authentication.getName()).ifPresent(user -> {
                model.addAttribute("userName", user.getName());
                model.addAttribute("userEmail", user.getEmail());
            });
            model.addAttribute("isDemo", demoAccountPolicy.isDemo(authentication));
        } else {
            model.addAttribute("userName", null);
            model.addAttribute("userEmail", null);
            model.addAttribute("isDemo", false);
        }
    }

    private Item getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean ownsItem(Item item, Authentication authentication) {
        return authentication != null
                && item.getSellerEmail() != null
                && item.getSellerEmail().equalsIgnoreCase(authentication.getName());
    }

    private String homeWithError(Model model, HttpSession session, String error) {
        return homeWithError(model, session, error, null);
    }

    private String homeWithError(Model model, HttpSession session, String error, Authentication authentication) {
        populateUserContext(model, authentication);
        prepareHomeModel(model, session, false);
        model.addAttribute("error", error);
        return "home_page";
    }

    private void prepareHomeModel(Model model, HttpSession session, boolean yorkOnly) {
        List<Item> items = itemRepository.findAll();
        if (yorkOnly) {
            items = items.stream().filter(Item::isSellerYorkVerified).toList();
        }
        model.addAttribute("items", items);
        model.addAttribute("yorkOnly", yorkOnly);
        addListingOptions(model);
        if (!model.containsAttribute("isAuthenticated")) {
            model.addAttribute("isAuthenticated", false);
        }
        if (!model.containsAttribute("isDemo")) {
            model.addAttribute("isDemo", false);
        }
        issueListingSubmissionToken(model, session);
    }

    private void addListingOptions(Model model) {
        model.addAttribute("listingConditions", listingPolicy.conditions());
        model.addAttribute("listingLocations", listingPolicy.locations());
    }

    private void issueListingSubmissionToken(Model model, HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute(LISTING_SUBMISSION_TOKEN, token);
        model.addAttribute("listingSubmissionToken", token);
    }

    private boolean consumeListingSubmissionToken(HttpSession session, String submittedToken) {
        synchronized (session) {
            Object expectedToken = session.getAttribute(LISTING_SUBMISSION_TOKEN);
            if (!(expectedToken instanceof String token) || !token.equals(submittedToken)) {
                return false;
            }
            session.removeAttribute(LISTING_SUBMISSION_TOKEN);
            return true;
        }
    }

}
