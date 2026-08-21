package com.autopro.backend.controller;

import com.autopro.backend.dto.review.CreateReviewRequest;
import com.autopro.backend.dto.review.ReviewResponse;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/mechanics/{mechanicId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Avis mécanicien", description = "Gestion des avis et de la réputation des mécaniciens")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Laisser un avis pour un mécanicien",
               description = "Un utilisateur ne peut laisser qu'un seul avis par mécanicien. "
                           + "La réputation du mécanicien (note moyenne, nombre d'avis) est recalculée automatiquement.")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long mechanicId,
            @Valid @RequestBody CreateReviewRequest request,
            Principal principal) {
        User reviewer = resolveUser(principal);
        return ResponseEntity.ok(reviewService.createReview(mechanicId, reviewer, request));
    }

    @GetMapping
    @Operation(summary = "Lister les avis d'un mécanicien, paginé")
    public ResponseEntity<List<ReviewResponse>> getReviews(
            @PathVariable Long mechanicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getMechanicReviews(mechanicId, page, size));
    }

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
