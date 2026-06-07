package at.ac.tuwien.inso.ctrl.session.scheduler;

import at.ac.tuwien.inso.ctrl.session.repository.SessionConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SessionConfigScheduler {
    private final SessionConfigRepository sessionConfigRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void rollOverSessionLimits() {
        sessionConfigRepository.applyNextDayLimits();
    }
}
