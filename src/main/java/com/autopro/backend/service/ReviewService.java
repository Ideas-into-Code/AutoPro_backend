package com.autopro.backend.service;

import com.autopro.backend.dto.review.CreateReviewRequest;
import com.autopro.backend.dto.review.ReviewResponse;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.Review;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MechanicRepository mechanicRepository;

    @Transactional
    public ReviewResponse createReview(Long mechanicId, User reviewer, CreateReviewRequest request) {
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new IllegalArgumentException("Mécanicien introuvable"));

        if (reviewRepository.existsByMechanicIdAndReviewerId(mechanicId, reviewer.getId())) {
            throw new IllegalArgumentException("Vous avez déjà laissé un avis pour ce mécanicien");
        }

        Review review = Review.builder()
                .mechanic(mechanic)
                .reviewer(reviewer)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        review = reviewRepository.save(review);

        updateMechanicReputation(mechanic);

        return toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMechanicReviews(Long mechanicId, int page, int size) {
        return reviewRepository.findByMechanicIdOrderByCreatedAtDesc(mechanicId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::toReviewResponse)
                .collect(Collectors.toList());
    }

    private void updateMechanicReputation(Mechanic mechanic) {
        Double average = reviewRepository.averageRatingByMechanicId(mechanic.getId());
        long count = reviewRepository.countByMechanicId(mechanic.getId());

        BigDecimal averageRating = average != null
                ? BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        mechanic.setAverageRating(averageRating);
        mechanic.setReviewCount((int) count);
        mechanicRepository.save(mechanic);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .mechanicId(review.getMechanic().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
