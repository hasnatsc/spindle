package com.asg.spindleserp.budget;

import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_notes",
        indexes = @Index(name = "idx_bgt_notes_budget", columnList = "budget_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetNote implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noted_by_user_id")
    private User notedByUser;

    @Column(name = "note_type", nullable = false, length = 20)
    @Builder.Default
    private String noteType = "COMMENT";
    // COMMENT | QUERY | ACTION_ITEM | APPROVAL_NOTE

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
    private String noteText;

    @Builder.Default
    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
