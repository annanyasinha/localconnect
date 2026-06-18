package com.localconnect.backend.util;

import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import com.localconnect.backend.enums.ApprovalStatus;
import com.localconnect.backend.enums.RoleName;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ServiceListingRepository serviceListingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByEmail("admin@localconnect.com")) {
            User admin = User.builder()
                    .fullName("System Admin")
                    .email("admin@localconnect.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .phone("9999999999")
                    .role(RoleName.ADMIN)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
        }

        Map<String, List<String>> categoryMap = new LinkedHashMap<>();

        categoryMap.put("Plumber", List.of(
                "Pipe Leakage Repair",
                "Tap Installation",
                "Bathroom Fitting",
                "Water Tank Cleaning",
                "Drain Blockage Fix"
        ));

        categoryMap.put("Electrician", List.of(
                "Switch Repair",
                "Fan Installation",
                "Wiring Work",
                "Light Fitting",
                "Inverter Support"
        ));

        categoryMap.put("Tutor", List.of(
                "Math Tuition",
                "Science Tuition",
                "English Tuition",
                "Exam Preparation",
                "Primary Classes"
        ));

        categoryMap.put("Househelp", List.of(
                "Dishwashing",
                "Brooming",
                "Mopping",
                "Dusting",
                "Laundry Help"
        ));

        categoryMap.put("Cook", List.of(
                "Daily Meal Cooking",
                "Tiffin Preparation",
                "Vegetarian Cooking",
                "Non-Veg Cooking",
                "Kitchen Assistance"
        ));

        categoryMap.put("Babysitter", List.of(
                "Infant Care",
                "Toddler Supervision",
                "Feeding Assistance",
                "Playtime Care",
                "Night Babysitting"
        ));

        categoryMap.put("Carpenter", List.of(
                "Furniture Repair",
                "Door Repair",
                "Window Fitting",
                "Shelf Installation",
                "Bed Assembly"
        ));

        categoryMap.put("Car Cleaner", List.of(
                "Exterior Wash",
                "Interior Vacuum",
                "Seat Cleaning",
                "Dashboard Cleaning",
                "Basic Detailing"
        ));

        categoryMap.put("Mason", List.of(
                "Wall Repair",
                "Brick Work",
                "Floor Tiling",
                "Plaster Work",
                "Small Construction"
        ));

        categoryMap.put("Pest Control", List.of(
                "Termite Control",
                "Mosquito Control",
                "Cockroach Treatment",
                "Rodent Control",
                "Home Sanitization"
        ));

        categoryMap.put("Beautician", List.of(
                "Bridal Makeup",
                "Party Makeup",
                "Hair Styling",
                "Facial Service",
                "Waxing / Grooming"
        ));

        categoryMap.put("Senior Care Support", List.of(
                "Daily Assistance",
                "Mobility Support",
                "Meal Support",
                "Medication Reminder",
                "Companionship Care"
        ));

        List<String> jamshedpurAreas = List.of(
                "Sakchi", "Bistupur", "Kadma", "Sonari",
                "Mango", "Golmuri", "Sidhgora", "Telco Colony",
                "Baridih", "Adityapur", "Jugsalai", "Dimna"
        );

        List<String> providerNames = List.of(
                "Aman Kumar", "Rohit Singh", "Neha Sharma", "Pooja Verma", "Rahul Das",
                "Sneha Gupta", "Vikash Prasad", "Deepak Mishra", "Shalini Sahu", "Ankit Tiwari",
                "Riya Kumari", "Abhishek Kumar", "Manish Singh", "Pankaj Das", "Sanjay Sharma"
        );

        for (Map.Entry<String, List<String>> entry : categoryMap.entrySet()) {
            String category = entry.getKey();
            List<String> subCategories = entry.getValue();

            for (String subCategory : subCategories) {
                for (int i = 1; i <= 5; i++) {
                    final String finalCategory = category;
                    final String finalSubCategory = subCategory;
                    final int finalI = i;

                    String email = (
                            finalCategory.toLowerCase().replace(" ", "").replace("/", "") +
                                    finalSubCategory.toLowerCase().replace(" ", "").replace("/", "") +
                                    finalI + "@local.com"
                    );

                    String phone = String.format(
                            "9%09d",
                            Math.abs((finalCategory + finalSubCategory + finalI).hashCode()) % 1_000_000_000
                    );

                    final String finalEmail = email;
                    final String finalPhone = phone;

                    String providerName = providerNames.get(
                            Math.abs((finalCategory + finalSubCategory + finalI).hashCode()) % providerNames.size()
                    );

                    String areaName = jamshedpurAreas.get(
                            Math.abs((finalCategory + finalSubCategory + finalI).hashCode()) % jamshedpurAreas.size()
                    );

                    User provider = userRepository.findByEmail(finalEmail).orElseGet(() -> {
                        User newProvider = User.builder()
                                .fullName(providerName)
                                .email(finalEmail)
                                .password(passwordEncoder.encode("123456"))
                                .phone(finalPhone)
                                .role(RoleName.PROVIDER)
                                .enabled(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                        return userRepository.save(newProvider);
                    });

                    boolean exists = false;
                    List<ServiceListing> providerServices = serviceListingRepository.findByProvider(provider);

                    for (ServiceListing service : providerServices) {
                        if (service.getCategory().equalsIgnoreCase(finalCategory)
                                && service.getSubCategory().equalsIgnoreCase(finalSubCategory)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        String serviceTitle = buildServiceTitle(finalCategory, finalSubCategory, finalI);

                        ServiceListing service = ServiceListing.builder()
                                .provider(provider)
                                .title(serviceTitle)
                                .category(finalCategory)
                                .subCategory(finalSubCategory)
                                .description(buildDescription(finalSubCategory))
                                .price(BigDecimal.valueOf(300 + (finalI * 50)))
                                .city("Jamshedpur")
                                .area(areaName)
                                .available(true)
                                .imageUrl("")
                                .approvalStatus(ApprovalStatus.APPROVED)
                                .createdAt(LocalDateTime.now())
                                .build();

                        serviceListingRepository.save(service);
                    }
                }
            }
        }

        System.out.println("Dummy data loaded successfully");
    }

    private String buildServiceTitle(String category, String subCategory, int index) {
        return switch (category) {
            case "Tutor" -> switch (subCategory) {
                case "Math Tuition" -> "Home Maths Tutor";
                case "Science Tuition" -> "Science Home Tutor";
                case "English Tuition" -> "English Language Tutor";
                case "Exam Preparation" -> "Exam Preparation Tutor";
                case "Primary Classes" -> "Primary Class Tutor";
                default -> subCategory + " Service";
            };
            case "Plumber" -> switch (subCategory) {
                case "Pipe Leakage Repair" -> "Pipe Repair Specialist";
                case "Tap Installation" -> "Tap Installation Expert";
                case "Bathroom Fitting" -> "Bathroom Fitting Service";
                case "Water Tank Cleaning" -> "Water Tank Cleaning Service";
                case "Drain Blockage Fix" -> "Drain Blockage Repair";
                default -> subCategory + " Service";
            };
            case "Electrician" -> switch (subCategory) {
                case "Switch Repair" -> "Switch Repair Electrician";
                case "Fan Installation" -> "Fan Installation Service";
                case "Wiring Work" -> "Home Wiring Electrician";
                case "Light Fitting" -> "Light Fitting Service";
                case "Inverter Support" -> "Inverter Support Electrician";
                default -> subCategory + " Service";
            };
            case "Beautician" -> switch (subCategory) {
                case "Bridal Makeup" -> "Bridal Makeup Artist";
                case "Party Makeup" -> "Party Makeup Service";
                case "Hair Styling" -> "Professional Hair Stylist";
                case "Facial Service" -> "Home Facial Service";
                case "Waxing / Grooming" -> "Waxing and Grooming Service";
                default -> subCategory + " Service";
            };
            default -> subCategory + " Service " + index;
        };
    }

    private String buildDescription(String subCategory) {
        return "Professional " + subCategory + " service by an experienced and trusted provider in Jamshedpur.";
    }
}