package com.palpal.dealightbe.domain.order.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.transaction.annotation.Transactional;

import com.palpal.dealightbe.domain.member.domain.Member;
import com.palpal.dealightbe.domain.member.domain.MemberRepository;
import com.palpal.dealightbe.domain.order.application.dto.request.OrderStatusUpdateReq;
import com.palpal.dealightbe.domain.order.application.dto.response.OrderRes;
import com.palpal.dealightbe.domain.order.application.dto.response.OrderStatusUpdateRes;
import com.palpal.dealightbe.domain.order.application.dto.response.OrdersRes;
import com.palpal.dealightbe.domain.order.domain.Order;
import com.palpal.dealightbe.domain.order.domain.OrderRepository;
import com.palpal.dealightbe.domain.order.domain.OrderStatus;
import com.palpal.dealightbe.domain.store.domain.DayOff;
import com.palpal.dealightbe.domain.store.domain.Store;
import com.palpal.dealightbe.domain.store.domain.StoreRepository;
import com.palpal.dealightbe.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@Transactional
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private StoreRepository storeRepository;

	@InjectMocks
	private OrderService orderService;

	public Member member;
	public Member storeOwner;
	public Store store;
	public Order order;

	public static final long MEMBER_ID = 1234L;
	public static final long STORE_OWNER_ID = 1235L;

	@BeforeEach
	void beforeEach() {
		member = Member.builder()
			.providerId(MEMBER_ID)
			.build();

		storeOwner = Member.builder()
			.providerId(STORE_OWNER_ID)
			.build();

		store = Store.builder()
			.name("GS25")
			.storeNumber("12341432")
			.telephone("022341321")
			.openTime(LocalTime.of(9, 0))
			.closeTime(LocalTime.of(18, 0))
			.dayOff(Set.of(DayOff.SAT, DayOff.SUN))
			.build();

		store.updateMember(storeOwner);

	}

	@Nested
	@DisplayName("<주문 상태 변경>")
	class UpdateStatusTest {
		@Nested
		@DisplayName("고객")
		class Member {
			@Test
			@DisplayName("'주문 확인' 상태에서 주문을 취소 할 수 있다")
			void member_statusUpdate_ReceivedToCanceled() {
				// given
				order = createOrder();

				when(memberRepository.findMemberByProviderId(MEMBER_ID))
					.thenReturn(Optional.of(member));

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));

				OrderStatus changedStatus = OrderStatus.CANCELED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				OrderStatusUpdateRes orderStatusUpdateRes
					= orderService.updateStatus(0L, orderStatusUpdateReq, MEMBER_ID);

				// then
				assertThat(orderStatusUpdateRes.status(), is(equalTo(changedStatus.name())));
				assertThat(order.getOrderStatus(), is(equalTo(changedStatus)));
			}

			@Test
			@DisplayName("'주문 접수' 상태 에서 주문을 취소 할 수 있다")
			void member_statusUpdate_ConfirmedToCanceled() {
				// given
				order = createOrder();

				when(memberRepository.findMemberByProviderId(MEMBER_ID))
					.thenReturn(Optional.of(member));

				when(memberRepository.findMemberByProviderId(STORE_OWNER_ID))
					.thenReturn(Optional.of(storeOwner));

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));

				orderService.updateStatus(0L, new OrderStatusUpdateReq("CONFIRMED"), STORE_OWNER_ID);

				OrderStatus changedStatus = OrderStatus.CANCELED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				// then
				assertThat(order.getOrderStatus(), is(equalTo(OrderStatus.CONFIRMED)));

				OrderStatusUpdateRes orderStatusUpdateRes
					= orderService.updateStatus(0L, orderStatusUpdateReq, MEMBER_ID);

				assertThat(orderStatusUpdateRes.status(), is(equalTo(changedStatus.name())));
				assertThat(order.getOrderStatus(), is(equalTo(changedStatus)));
			}

			@Test
			@DisplayName("'주문 확인' 상태 에서 '주문 접수'로 상태를 변경할 수 없다")
			void member_statusUpdateFailed_toConfirmed() {
				// given
				order = createOrder();

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));
				when(memberRepository.findMemberByProviderId(MEMBER_ID))
					.thenReturn(Optional.of(member));

				OrderStatus changedStatus = OrderStatus.CONFIRMED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				// then
				assertThrows(BusinessException.class, () -> {
					orderService.updateStatus(0L, orderStatusUpdateReq, MEMBER_ID);
				});
			}

			@Test
			@DisplayName("'주문 확인' 상태 에서 '주문 완료'로 상태를 변경할 수 없다")
			void member_statusUpdateFailed_toCompleted() {
				// given
				order = createOrder();

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));
				when(memberRepository.findMemberByProviderId(MEMBER_ID))
					.thenReturn(Optional.of(member));

				OrderStatus changedStatus = OrderStatus.COMPLETED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				// then
				assertThrows(BusinessException.class, () -> {
					orderService.updateStatus(0L, orderStatusUpdateReq, MEMBER_ID);
				});
			}
		}

		@Nested
		@DisplayName("업체")
		class Store {
			@Test
			@DisplayName("'주문 확인'에서 '주문 접수'로 상태를 변경 할 수 있다")
			void store_statusUpdate_receivedToConfirmed() {
				// given
				order = createOrder();

				when(memberRepository.findMemberByProviderId(STORE_OWNER_ID))
					.thenReturn(Optional.of(storeOwner));

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));

				OrderStatus changedStatus = OrderStatus.CONFIRMED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				OrderStatusUpdateRes orderStatusUpdateRes
					= orderService.updateStatus(0L, orderStatusUpdateReq, STORE_OWNER_ID);

				// then
				assertThat(orderStatusUpdateRes.status(), is(equalTo(changedStatus.name())));
				assertThat(order.getOrderStatus(), is(equalTo(changedStatus)));
			}

			@Test
			@DisplayName("'주문 접수'에서 '주문 완료'로 상태를 변경 할 수 있다")
			void store_statusUpdate_confirmedToCompleted() {
				// given

				order = createOrder();
				when(memberRepository.findMemberByProviderId(STORE_OWNER_ID))
					.thenReturn(Optional.of(storeOwner));

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));

				orderService.updateStatus(0L, new OrderStatusUpdateReq("CONFIRMED"), STORE_OWNER_ID);

				OrderStatus changedStatus = OrderStatus.COMPLETED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				// then
				assertThat(order.getOrderStatus(), is(equalTo(OrderStatus.CONFIRMED)));

				OrderStatusUpdateRes orderStatusUpdateRes
					= orderService.updateStatus(0L, orderStatusUpdateReq, STORE_OWNER_ID);

				assertThat(orderStatusUpdateRes.status(), is(equalTo(changedStatus.name())));
				assertThat(order.getOrderStatus(), is(equalTo(changedStatus)));
			}

			@Test
			@DisplayName("'주문 확인' 상태에서 주문을 취소할 수 있다")
			void store_statusUpdate_receivedToCanceled() {
				// given
				order = createOrder();

				when(memberRepository.findMemberByProviderId(STORE_OWNER_ID))
					.thenReturn(Optional.of(storeOwner));

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));

				OrderStatus changedStatus = OrderStatus.CANCELED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				OrderStatusUpdateRes orderStatusUpdateRes
					= orderService.updateStatus(0L, orderStatusUpdateReq, STORE_OWNER_ID);

				// then
				assertThat(orderStatusUpdateRes.status(), is(equalTo(changedStatus.name())));
				assertThat(order.getOrderStatus(), is(equalTo(changedStatus)));
			}

			@Test
			@DisplayName("'주문 접수' 상태에서 주문을 취소할 수 있다")
			void store_statusUpdate_confirmedToCanceled() {
				// given
				order = createOrder();

				when(memberRepository.findMemberByProviderId(STORE_OWNER_ID))
					.thenReturn(Optional.of(storeOwner));

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));

				orderService.updateStatus(0L, new OrderStatusUpdateReq("CONFIRMED"), STORE_OWNER_ID);

				OrderStatus changedStatus = OrderStatus.CANCELED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				// then
				assertThat(order.getOrderStatus(), is(equalTo(OrderStatus.CONFIRMED)));

				OrderStatusUpdateRes orderStatusUpdateRes
					= orderService.updateStatus(0L, orderStatusUpdateReq, STORE_OWNER_ID);

				assertThat(orderStatusUpdateRes.status(), is(equalTo(changedStatus.name())));
				assertThat(order.getOrderStatus(), is(equalTo(changedStatus)));
			}

			@Test
			@DisplayName("'주문 확인' 상태 에서 주문 접수 없이 바로 '주문 완료'로 상태를 변경할 수 없다")
			void store_statusUpdateFailed_miss() {
				// given
				order = createOrder();

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));
				when(memberRepository.findMemberByProviderId(STORE_OWNER_ID))
					.thenReturn(Optional.of(storeOwner));

				OrderStatus changedStatus = OrderStatus.COMPLETED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				// then
				assertThrows(BusinessException.class, () -> {
					orderService.updateStatus(0L, orderStatusUpdateReq, STORE_OWNER_ID);
				});
			}

			@Test
			@DisplayName("'주문 접수' 상태 에서 '주문 확인' 인 이전 상태로 되돌아 갈 수 없다")
			void store_statusUpdateFailed_undo() {
				// given
				order = createOrder();

				when(orderRepository.findById(anyLong()))
					.thenReturn(Optional.of(order));
				when(memberRepository.findMemberByProviderId(STORE_OWNER_ID))
					.thenReturn(Optional.of(storeOwner));

				orderService.updateStatus(0L, new OrderStatusUpdateReq("CONFIRMED"), STORE_OWNER_ID);

				OrderStatus changedStatus = OrderStatus.RECEIVED;
				OrderStatusUpdateReq orderStatusUpdateReq = new OrderStatusUpdateReq(changedStatus.name());

				// when
				// then

				assertThat(order.getOrderStatus(), is(equalTo(OrderStatus.CONFIRMED)));

				assertThrows(BusinessException.class, () -> {
					orderService.updateStatus(0L, orderStatusUpdateReq, STORE_OWNER_ID);
				});
			}
		}
	}

	@Nested
	@DisplayName("<업체의 주문 목록 조회>")
	class findByStoreIdTest {
		@Nested
		@DisplayName("성공")
		class Success {
			@DisplayName("업체의 주문 목록을 조회할 수 있다")
			@Test
			void findByStoreId_success() {
				// given
				long storeId = 1L;

				Order order1 = createOrder(LocalTime.of(18, 30), 30000);
				Order order2 = createOrder(LocalTime.of(22, 0), 10000);

				Slice<Order> ordersSlice = new SliceImpl<>(
					Arrays.asList(order1, order2), PageRequest.of(0, 10), true
				);

				when(orderRepository.findAllByStoreId(anyLong(), any(), any()))
					.thenReturn(ordersSlice);

				when(storeRepository.findById(storeId))
					.thenReturn(Optional.ofNullable(store));

				// when
				OrdersRes result = orderService.findAllByStoreId(storeId, storeOwner.getProviderId(), null,
					PageRequest.of(0, 10));

				// then
				assertThat(result.orders(), hasSize(2));
				assertThat(result.hasNext(), is(true));

				Assertions.assertThat(result.orders().get(0))
					.usingRecursiveComparison()
					.isEqualTo(OrderRes.from(order1));

				Assertions.assertThat(result.orders().get(1))
					.usingRecursiveComparison()
					.isEqualTo(OrderRes.from(order2));

			}
		}

		@Nested
		@DisplayName("실패")
		class fail {
			@DisplayName("업체의 주인이 아니면 주문 목록을 조회할 수 없다")
			@Test
			void findByStoreId_fail_not_owner() {
				// given
				long storeId = 1L;
				long requesterId = member.getProviderId();

				when(storeRepository.findById(storeId))
					.thenReturn(Optional.ofNullable(store));

				// when
				// then
				assertThrows(BusinessException.class, () ->
					orderService.findAllByStoreId(storeId, requesterId, null, null));
			}
		}
	}

	@Nested
	@DisplayName("<고객의 주문 목록 조회>")
	class findAllByMemberProviderIdTest {
		@Nested
		@DisplayName("성공")
		class Success {
			@DisplayName("주문 이력을 조회할 수 있다")
			@Test
			void findAllByMember_success() {
				// given
				long memberProviderId = 1L;

				Order order1 = createOrder(LocalTime.of(18, 30), 30000);
				Order order2 = createOrder(LocalTime.of(22, 0), 10000);

				Slice<Order> ordersSlice = new SliceImpl<>(
					Arrays.asList(order1, order2), PageRequest.of(0, 10), true
				);

				when(orderRepository.findAllByMemberProviderId(anyLong(), any(), any()))
					.thenReturn(ordersSlice);

				when(memberRepository.findMemberByProviderId(memberProviderId))
					.thenReturn(Optional.ofNullable(member));

				// when
				OrdersRes result = orderService.findAllByMemberProviderId(memberProviderId, null,
					PageRequest.of(0, 10));

				// then
				assertThat(result.orders(), hasSize(2));
				assertThat(result.hasNext(), is(true));

				Assertions.assertThat(result.orders().get(0))
					.usingRecursiveComparison()
					.isEqualTo(OrderRes.from(order1));
				Assertions.assertThat(result.orders().get(1))
					.usingRecursiveComparison()
					.isEqualTo(OrderRes.from(order2));
			}
		}
	}

	private Order createOrder(LocalTime arrivalTime, int totalPrice) {
		return Order.builder()
			.demand("도착할 때까지 상품 냉장고에 보관 부탁드려요")
			.arrivalTime(arrivalTime)
			.store(store)
			.member(member)
			.totalPrice(totalPrice)
			.build();
	}

	private Order createOrder() {
		return Order.builder()
			.demand("도착할 때까지 상품 냉장고에 보관 부탁드려요")
			.arrivalTime(LocalTime.of(12, 30))
			.store(store)
			.member(member)
			.totalPrice(10000)
			.build();
	}
}
