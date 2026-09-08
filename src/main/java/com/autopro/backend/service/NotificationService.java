package com.autopro.backend.service;

import com.autopro.backend.dto.notification.NotificationResponse;
import com.autopro.backend.entity.Notification;
import com.autopro.backend.entity.NotificationType;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.NotificationRepository;
import com.autopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Création et distribution des notifications utilisateur.
 *
 * <p>Appelé par les services métier lors d'un événement. La création d'une
 * notification ne doit jamais faire échouer l'opération principale : les
 * méthodes {@code notify*} avalent leurs erreurs (log seulement).</p>
 *
 * <p>Poussée temps réel : {@code convertAndSendToUser(email, "/queue/notifications", …)}
 * → le client abonné à {@code /user/queue/notifications} la reçoit.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /** Crée et pousse une notification. Ne lève jamais. */
    @Transactional
    public void notify(User recipient, NotificationType type, String title, String body, String link) {
        if (recipient == null) {
            return;
        }
        try {
            Notification saved = notificationRepository.save(Notification.builder()
                    .recipient(recipient)
                    .type(type)
                    .title(title)
                    .body(body)
                    .link(link)
                    .read(false)
                    .build());
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(), "/queue/notifications", NotificationResponse.from(saved));
        } catch (RuntimeException ex) {
            log.warn("Notification non délivrée à {} : {}", recipient.getId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(String email, boolean unreadOnly, int page, int size) {
        Long userId = getUser(email).getId();
        PageRequest pr = PageRequest.of(page, size);
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId, pr)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pr);
        return result.getContent().stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByRecipientIdAndReadFalse(getUser(email).getId());
    }

    @Transactional
    public void markRead(String email, Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new com.autopro.backend.exception.ResourceNotFoundException(
                        "Notification introuvable: " + id));
        if (!n.getRecipient().getId().equals(getUser(email).getId())) {
            throw new SecurityException("Cette notification ne vous appartient pas");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(String email) {
        notificationRepository.markAllReadForRecipient(getUser(email).getId());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
    }
}
