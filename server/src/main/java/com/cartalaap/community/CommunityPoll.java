package com.cartalaap.community;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "community_polls")
public class CommunityPoll {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "message_id", unique = true) private CommunityMessage message;
    @Column(nullable = false, length = 300) private String question;
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") private Set<CommunityPollOption> options = new LinkedHashSet<>();
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true) private Set<CommunityPollVote> votes = new HashSet<>();

    protected CommunityPoll() {}
    public CommunityPoll(CommunityMessage message, String question) { this.message = message; this.question = question; }
    public void addOption(String text, int position) { options.add(new CommunityPollOption(this, text, position)); }
    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public Set<CommunityPollOption> getOptions() { return options; }
    public Set<CommunityPollVote> getVotes() { return votes; }
}
