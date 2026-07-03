package io.crest.share.manage;

import io.crest.api.share.request.TicketCreator;
import io.crest.api.share.request.TicketDelRequest;
import io.crest.api.share.request.TicketSwitchRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.share.dao.auto.entity.CoreShare;
import io.crest.share.dao.auto.entity.CoreShareTicket;
import io.crest.share.dao.auto.mapper.CoreShareMapper;
import io.crest.share.dao.auto.mapper.CoreShareTicketMapper;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShareTicketManageSecurityTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void saveTicketRejectsTicketOwnedByAnotherUserBeforeUpdating() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreShareTicketMapper ticketMapper = mock(CoreShareTicketMapper.class);
        CoreShareMapper shareMapper = mock(CoreShareMapper.class);
        ShareTicketManage manage = manage(ticketMapper, shareMapper);

        CoreShareTicket ticket = ticket("uuid-owner");
        CoreShare share = share(3L);
        when(ticketMapper.selectOne(any())).thenReturn(ticket);
        when(shareMapper.selectOne(any())).thenReturn(share);

        TicketCreator creator = new TicketCreator();
        creator.setTicket("ticket01");
        creator.setUuid("uuid-owner");

        CrestException exception = assertThrows(CrestException.class, () -> manage.saveTicket(creator));

        assertEquals("分享资源不存在或无访问权限", exception.getMsg());
        verify(ticketMapper, never()).deleteById(any(CoreShareTicket.class));
        verify(ticketMapper, never()).insert(any(CoreShareTicket.class));
    }

    @Test
    void deleteTicketRejectsTicketOwnedByAnotherUserBeforeDeleting() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreShareTicketMapper ticketMapper = mock(CoreShareTicketMapper.class);
        CoreShareMapper shareMapper = mock(CoreShareMapper.class);
        ShareTicketManage manage = manage(ticketMapper, shareMapper);

        when(ticketMapper.selectOne(any())).thenReturn(ticket("uuid-owner"));
        when(shareMapper.selectOne(any())).thenReturn(share(3L));
        TicketDelRequest request = new TicketDelRequest();
        request.setTicket("ticket01");

        CrestException exception = assertThrows(CrestException.class, () -> manage.deleteTicket(request));

        assertEquals("分享资源不存在或无访问权限", exception.getMsg());
        verify(ticketMapper, never()).delete(any());
    }

    @Test
    void switchRequireRejectsResourceOwnedByAnotherUserBeforeUpdating() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreShareTicketMapper ticketMapper = mock(CoreShareTicketMapper.class);
        CoreShareMapper shareMapper = mock(CoreShareMapper.class);
        ShareTicketManage manage = manage(ticketMapper, shareMapper);

        when(shareMapper.selectOne(any())).thenReturn(null);
        TicketSwitchRequest request = new TicketSwitchRequest();
        request.setResourceId("20");
        request.setRequire(true);

        CrestException exception = assertThrows(CrestException.class, () -> manage.switchRequire(request));

        assertEquals("分享资源不存在或无访问权限", exception.getMsg());
        verify(shareMapper, never()).updateById(any(CoreShare.class));
    }

    private ShareTicketManage manage(CoreShareTicketMapper ticketMapper, CoreShareMapper shareMapper) {
        ShareTicketManage manage = new ShareTicketManage();
        ReflectionTestUtils.setField(manage, "coreShareTicketMapper", ticketMapper);
        ReflectionTestUtils.setField(manage, "coreShareMapper", shareMapper);
        return manage;
    }

    private CoreShareTicket ticket(String uuid) {
        CoreShareTicket ticket = new CoreShareTicket();
        ticket.setId(10L);
        ticket.setTicket("ticket01");
        ticket.setUuid(uuid);
        return ticket;
    }

    private CoreShare share(Long creator) {
        CoreShare share = new CoreShare();
        share.setId(20L);
        share.setCreator(creator);
        share.setUuid("uuid-owner");
        return share;
    }
}
