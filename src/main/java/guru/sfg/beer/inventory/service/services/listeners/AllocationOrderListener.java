package guru.sfg.beer.inventory.service.services.listeners;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.beer.inventory.service.services.AllocationService;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocationOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocationOrderListener {

    private final AllocationService allocationService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listenAllocation(AllocateOrderRequest allocateOrderRequest) {

        BeerOrderDto beerOrderDto = allocateOrderRequest.getBeerOrderDto();

        AllocationOrderResponse.AllocationOrderResponseBuilder  allocationOrderResponseBuilder = AllocationOrderResponse.builder();

        allocationOrderResponseBuilder.beerOrderDto(beerOrderDto);

        try {
            boolean succeeded = allocationService.allocateOrder(beerOrderDto);

            if (succeeded){
                allocationOrderResponseBuilder.pendingInventory(false);
            } else {
                allocationOrderResponseBuilder.pendingInventory(true);
            }

            allocationOrderResponseBuilder.allocationError(false);
        }catch (Exception e){
            log.error("Inventory allocation failed for " + beerOrderDto.getId(), e.getMessage());
            allocationOrderResponseBuilder.allocationError(true);
        }

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, allocationOrderResponseBuilder.build());
    }
}
