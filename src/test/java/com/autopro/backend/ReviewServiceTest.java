package com.autopro.backend;

import com.autopro.backend.dto.review.CreateReviewRequest;
import com.autopro.backend.dto.review.ReviewResponse;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.Review;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.ReviewRepository;
import com.autopro.backend.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User buildUser(Long id) {
        return User.builder().id(id).firstName("Jean").lastName("Dupont").email("jean" + id + "@example.com").build();
    }

    private Mechanic buildMechanic(Long id) {
        return Mechanic.builder().id(id).averageRating(BigDecimal.ZERO).reviewCount(0).build();
    }

    @Test
    void createReview_savesReviewAndUpdatesReputation() {
        Mechanic mechanic = buildMechanic(1L);
        User reviewer = buildUser(2L);
        CreateReviewRequest request = new CreateReviewRequest(5, "Excellent travail");

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));
        when(reviewRepository.existsByMechanicIdAndReviewerId(1L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(reviewRepository.averageRatingByMechanicId(1L)).thenReturn(5.0);
        when(reviewRepository.countByMechanicId(1L)).thenReturn(1L);
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        ReviewResponse response = reviewService.createReview(1L, reviewer, request);

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getReviewerId()).isEqualTo(2L);
        assertThat(mechanic.getAverageRating()).isEqualByComparingTo("5.00");
        assertThat(mechanic.getReviewCount()).isEqualTo(1);
        verify(mechanicRepository).save(mechanic);
    }

    @Test
    void createReview_throwsWhenMechanicNotFound() {
        User reviewer = buildUser(2L);
        CreateReviewRequest request = new CreateReviewRequest(4, null);
        when(mechanicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(99L, reviewer, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_throwsWhenReviewerAlreadyReviewedThisMechanic() {
        Mechanic mechanic = buildMechanic(1L);
        User reviewer = buildUser(2L);
        CreateReviewRequest request = new CreateReviewRequest(3, "Correct");

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));
        when(reviewRepository.existsByMechanicIdAndReviewerId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(1L, reviewer, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà laissé un avis");

        verify(reviewRepository, never()).save(any());
        verify(mechanicRepository, never()).save(any());
    }

    @Test
    void createReview_setsZeroAverageWhenNoRatingsReturned() {
        Mechanic mechanic = buildMechanic(1L);
        User reviewer = buildUser(2L);
        CreateReviewRequest request = new CreateReviewRequest(1, null);

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));
        when(reviewRepository.existsByMechanicIdAndReviewerId(1L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.averageRatingByMechanicId(1L)).thenReturn(null);
        when(reviewRepository.countByMechanicId(1L)).thenReturn(0L);
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        reviewService.createReview(1L, reviewer, request);

        assertThat(mechanic.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mechanic.getReviewCount()).isEqualTo(0);
    }

    @Test
    void getMechanicReviews_returnsMappedPage() {
        User reviewer = buildUser(2L);
        Mechanic mechanic = buildMechanic(1L);
        Review review = Review.builder().id(10L).mechanic(mechanic).reviewer(reviewer).rating(4).comment("Bien").build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);

        when(reviewRepository.findByMechanicIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        List<ReviewResponse> result = reviewService.getMechanicReviews(1L, 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualTo(4);
        assertThat(result.get(0).getReviewerName()).isEqualTo("Jean Dupont");
    }
}
