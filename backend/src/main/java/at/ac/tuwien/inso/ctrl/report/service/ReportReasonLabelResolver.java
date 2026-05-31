package at.ac.tuwien.inso.ctrl.report.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReportReasonLabelResolver {
  private static final Map<String, String> REASON_LABELS =
      Map.ofEntries(
          Map.entry("bullying", "Bullying or Harassment"),
          Map.entry("self_harm", "Self harm"),
          Map.entry("violence", "Violence"),
          Map.entry("illegal", "Illegal or Restricted items"),
          Map.entry("nudity", "Nudity or sexual content"),
          Map.entry("copyright", "Scam or Fraud"),
          Map.entry("misinformation", "Misinformation"),
          Map.entry("child_exploitation", "Child abuse, exploitation or endangerment"),
          Map.entry("non_consensual", "Non-consensual intimate images"),
          Map.entry("hate_speech", "Hate speech or discrimination"),
          Map.entry("terrorism", "Terrorism or violent extremism"),
          Map.entry("false_source", "Source does not fit to post"),
          Map.entry("other", "Other"));

  public String resolve(String reason) {
    if (reason == null || reason.isBlank()) {
      return "";
    }
    return REASON_LABELS.getOrDefault(reason, reason);
  }
}

