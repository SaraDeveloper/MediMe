#ifndef RELAY_H
#define RELAY_H

/**
 * @brief Initialize the relay GPIOs (call once at startup)
 */
void RelayInit(void);

/**
 * @brief Turn a relay ON
 * @param relayNumber Relay number: 1 or 2
 */
void RelayOn(int relayNumber);

/**
 * @brief Turn a relay OFF
 * @param relayNumber Relay number: 1 or 2
 */
void RelayOff(int relayNumber);

#endif /* RELAY_H */
