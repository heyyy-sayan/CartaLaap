package com.cartalaap.community;

import jakarta.persistence.*;

@Entity
@Table(name = "community_poll_options")
public class CommunityPollOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poll_id") private CommunityPoll poll;
    @Column(name = "option_text", nullable = false, length = 120) private String text;
    @Column(name = "option_position", nullable = false) private int position;

    protected CommunityPollOption() {}
    CommunityPollOption(CommunityPoll poll, String text, int position) { this.poll = poll; this.text = text; this.position = position; }
    public Long getId() { return id; }
    public CommunityPoll getPoll() { return poll; }
    public String getText() { return text; }
}
