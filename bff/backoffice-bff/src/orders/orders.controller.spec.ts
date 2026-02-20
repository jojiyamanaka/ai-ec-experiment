import { OrdersController } from './orders.controller';

describe('OrdersController', () => {
  const ordersService = {
    getOrders: jest.fn(),
    getOrderById: jest.fn(),
    updateOrderStatus: jest.fn(),
    retryAllocation: jest.fn(),
  };

  let controller: OrdersController;

  beforeEach(() => {
    jest.clearAllMocks();
    controller = new OrdersController(ordersService as never);
  });

  it('confirmOrder converts id to number and forwards CONFIRMED status', async () => {
    ordersService.updateOrderStatus.mockResolvedValue({ success: true, data: {} });

    await controller.confirmOrder('12', { token: 'token-1', traceId: 'trace-1' });

    expect(ordersService.updateOrderStatus).toHaveBeenCalledWith(12, 'CONFIRMED', 'token-1', 'trace-1');
  });

  it('updateOrderStatus forwards status from request body', async () => {
    ordersService.updateOrderStatus.mockResolvedValue({ success: true, data: {} });

    await controller.updateOrderStatus('15', { status: 'SHIPPED' }, { token: 'token-2', traceId: 'trace-2' });

    expect(ordersService.updateOrderStatus).toHaveBeenCalledWith(15, 'SHIPPED', 'token-2', 'trace-2');
  });

  it('retryAllocation converts id to number and forwards auth context', async () => {
    ordersService.retryAllocation.mockResolvedValue({ success: true, data: {} });

    await controller.retryAllocation('18', { token: 'token-3', traceId: 'trace-3' });

    expect(ordersService.retryAllocation).toHaveBeenCalledWith(18, 'token-3', 'trace-3');
  });
});
