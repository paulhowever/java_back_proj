package ru.tischenko.vk.service.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.AnalyzeTeamResponse;
import ru.tischenko.vk.api.dto.Dtos.AssignSubTeamRequest;
import ru.tischenko.vk.domain.Enums.DeliveryStatus;
import ru.tischenko.vk.domain.Enums.NotificationType;
import ru.tischenko.vk.domain.Enums.UserLevel;
import ru.tischenko.vk.domain.NotificationEntity;
import ru.tischenko.vk.domain.SubTeamEntity;
import ru.tischenko.vk.domain.TeamEntity;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.repository.NotificationRepository;
import ru.tischenko.vk.repository.SubTeamRepository;
import ru.tischenko.vk.repository.TeamRepository;
import ru.tischenko.vk.repository.UserRepository;
import ru.tischenko.vk.service.DomainEventService;
import ru.tischenko.vk.service.Exceptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeamOperationsService {
    private static final Logger log = LoggerFactory.getLogger(TeamOperationsService.class);

    private final SubTeamRepository subTeamRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DomainEventService eventService;

    public TeamOperationsService(SubTeamRepository subTeamRepository,
                                 TeamRepository teamRepository,
                                 UserRepository userRepository,
                                 NotificationRepository notificationRepository,
                                 DomainEventService eventService) {
        this.subTeamRepository = subTeamRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.eventService = eventService;
    }

    public record AssignSubTeamResultLog(boolean hasLead, Long warningNotificationId) {}

    @Transactional
    public AssignSubTeamResultLog assignUsersToSubTeam(AssignSubTeamRequest req) {
        SubTeamEntity subTeam = subTeamRepository.findById(req.subTeamId())
                .orElseThrow(() -> new Exceptions.NotFoundException("SubTeam not found"));
        List<UserEntity> users = userRepository.findAllById(req.userIds());
        if (users.size() != new HashSet<>(req.userIds()).size()) {
            throw new Exceptions.NotFoundException("One or more users not found");
        }
        subTeam.getMembers().clear();
        subTeam.getMembers().addAll(users);
        boolean hasLead = users.stream().anyMatch(u -> u.getLevel() == UserLevel.LEAD);
        Long warningNotificationId = null;
        if (!hasLead && !users.isEmpty()) {
            NotificationEntity n = new NotificationEntity();
            n.setUser(users.get(0));
            n.setType(NotificationType.WARNING);
            n.setText("SubTeam '" + subTeam.getName() + "' has no lead");
            n.setDeliveryStatus(DeliveryStatus.PENDING);
            notificationRepository.save(n);
            warningNotificationId = n.getId();
            eventService.publishNotificationCreated(n.getId());
        }
        log.info("assignUsersToSubTeam subTeamId={} users={} hasLead={}", req.subTeamId(), req.userIds().size(), hasLead);
        return new AssignSubTeamResultLog(hasLead, warningNotificationId);
    }

    @Transactional(readOnly = true)
    public AnalyzeTeamResponse analyzeTeam(Long teamId) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new Exceptions.NotFoundException("Team not found"));
        Map<UserLevel, Long> grouped = team.getMembers().stream()
                .collect(Collectors.groupingBy(UserEntity::getLevel, Collectors.counting()));
        long juniors = grouped.getOrDefault(UserLevel.JUNIOR, 0L);
        long middles = grouped.getOrDefault(UserLevel.MIDDLE, 0L);
        long leads = grouped.getOrDefault(UserLevel.LEAD, 0L);
        long total = juniors + middles + leads;
        boolean hasLead = leads > 0;

        List<String> issues = new ArrayList<>();
        if (!hasLead) issues.add("no lead");
        if (total == 0) issues.add("team is empty");
        if (total > 0 && juniors > 0 && (middles + leads) == 0) issues.add("only juniors, no senior staff");
        if (total >= 4 && leads == 0) issues.add("team of " + total + " has no lead");
        if (total >= 5 && (middles + leads) * 2 < juniors) issues.add("too many juniors relative to middles/leads");

        String recommendation = issues.isEmpty() ? "Composition looks balanced" : String.join("; ", issues);
        return new AnalyzeTeamResponse(hasLead, juniors, middles, leads, recommendation);
    }
}
