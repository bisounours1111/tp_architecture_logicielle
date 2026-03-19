import sys
import time
from datetime import datetime, timedelta

import requests

GATEWAY_URL = "http://localhost:8080"
ROOM_URL = f"{GATEWAY_URL}/api/rooms"
MEMBER_URL = f"{GATEWAY_URL}/api/members"
RESERVATION_URL = f"{GATEWAY_URL}/api/reservations"
TIMEOUT_SECONDS = 5
POLL_INTERVAL_SECONDS = 0.5
POLL_TIMEOUT_SECONDS = 10


class TestFailure(Exception):
    pass


session = requests.Session()


def log(message):
    print(message)


def fail(message):
    raise TestFailure(message)


def request(method, url, expected_statuses=None, **kwargs):
    response = session.request(method, url, timeout=TIMEOUT_SECONDS, **kwargs)
    if expected_statuses is not None and response.status_code not in expected_statuses:
        fail(
            f"{method} {url} -> statut {response.status_code}, attendu {expected_statuses}. "
            f"Reponse: {response.text}"
        )
    return response


def expect(condition, message):
    if not condition:
        fail(message)


def wait_until(description, predicate, timeout=POLL_TIMEOUT_SECONDS):
    deadline = time.time() + timeout
    last_error = None
    while time.time() < deadline:
        try:
            if predicate():
                return
        except Exception as exc:  # pragma: no cover - helper for flaky startup
            last_error = exc
        time.sleep(POLL_INTERVAL_SECONDS)
    if last_error is not None:
        fail(f"{description} a expire. Derniere erreur: {last_error}")
    fail(f"{description} a expire.")


def iso_slot(start_offset_minutes, duration_minutes):
    start = datetime.now() + timedelta(minutes=start_offset_minutes)
    end = start + timedelta(minutes=duration_minutes)
    return start.isoformat(), end.isoformat()


def active_slot_payload(room_id, member_id, start_offset_minutes=-5, duration_minutes=60):
    start, end = iso_slot(start_offset_minutes, duration_minutes)
    return {
        "roomId": room_id,
        "memberId": member_id,
        "startDateTime": start,
        "endDateTime": end,
    }


def create_room(name, city="Lyon", capacity=5, room_type="MEETING_ROOM", hourly_rate=40, available=True):
    payload = {
        "name": name,
        "city": city,
        "capacity": capacity,
        "type": room_type,
        "hourlyRate": hourly_rate,
        "available": available,
    }
    response = request("POST", ROOM_URL, expected_statuses={200, 201}, json=payload)
    return response.json()


def create_member(full_name, email, subscription_type="BASIC"):
    payload = {
        "fullName": full_name,
        "email": email,
        "subscriptionType": subscription_type,
    }
    response = request("POST", MEMBER_URL, expected_statuses={200, 201}, json=payload)
    return response.json()


def create_reservation(payload, expected_statuses=None):
    response = request("POST", RESERVATION_URL, expected_statuses=expected_statuses, json=payload)
    return response


def get_room(room_id, expected_statuses=None):
    response = request("GET", f"{ROOM_URL}/{room_id}", expected_statuses=expected_statuses)
    return response


def get_member(member_id, expected_statuses=None):
    response = request("GET", f"{MEMBER_URL}/{member_id}", expected_statuses=expected_statuses)
    return response


def get_reservation(reservation_id, expected_statuses=None):
    response = request("GET", f"{RESERVATION_URL}/{reservation_id}", expected_statuses=expected_statuses)
    return response


def unique_suffix():
    return str(int(time.time() * 1000))


def wait_for_gateway():
    log("0. Attente de disponibilite des services via l'API Gateway...")

    def gateway_ready():
        room_ok = request("GET", ROOM_URL).status_code == 200
        member_ok = request("GET", MEMBER_URL).status_code == 200
        reservation_ok = request("GET", RESERVATION_URL).status_code == 200
        return room_ok and member_ok and reservation_ok

    wait_until("Les routes du gateway ne repondent pas encore", gateway_ready, timeout=20)


def test_room_routes():
    suffix = unique_suffix()
    log("\n1. Verification Room Service...")

    rooms_before = request("GET", ROOM_URL, expected_statuses={200}).json()
    expect(isinstance(rooms_before, list), "GET /api/rooms doit renvoyer une liste")

    room = create_room(f"Salle CRUD {suffix}", city="Paris", capacity=8, hourly_rate=55)
    room_id = room["id"]
    expect(room["available"] is True, "Une salle creee doit etre disponible")

    fetched = get_room(room_id, expected_statuses={200}).json()
    expect(fetched["name"] == f"Salle CRUD {suffix}", "GET /api/rooms/{id} doit retourner la bonne salle")

    updated_payload = {
        "name": f"Salle CRUD Update {suffix}",
        "city": "Marseille",
        "capacity": 12,
        "type": "PRIVATE_OFFICE",
        "hourlyRate": 70,
        "available": True,
    }
    updated = request("PUT", f"{ROOM_URL}/{room_id}", expected_statuses={200}, json=updated_payload).json()
    expect(updated["city"] == "Marseille", "PUT /api/rooms/{id} doit mettre a jour la ville")

    request("PATCH", f"{ROOM_URL}/{room_id}/availability", expected_statuses={200}, params={"available": "false"})
    expect(get_room(room_id, expected_statuses={200}).json()["available"] is False, "PATCH availability=false a echoue")

    request("PATCH", f"{ROOM_URL}/{room_id}/availability", expected_statuses={200}, params={"available": "true"})
    expect(get_room(room_id, expected_statuses={200}).json()["available"] is True, "PATCH availability=true a echoue")

    get_room(999999999, expected_statuses={404})


def test_member_routes():
    suffix = unique_suffix()
    log("\n2. Verification Member Service...")

    members_before = request("GET", MEMBER_URL, expected_statuses={200}).json()
    expect(isinstance(members_before, list), "GET /api/members doit renvoyer une liste")

    member = create_member(f"Membre CRUD {suffix}", f"crud-{suffix}@test.com", "BASIC")
    member_id = member["id"]
    expect(member["maxConcurrentBookings"] == 2, "Un membre BASIC doit avoir un quota de 2")
    expect(member["suspended"] is False, "Un membre cree ne doit pas etre suspendu")

    fetched = get_member(member_id, expected_statuses={200}).json()
    expect(fetched["email"] == f"crud-{suffix}@test.com", "GET /api/members/{id} doit retourner le bon membre")

    updated_payload = {
        "fullName": f"Membre PRO {suffix}",
        "email": f"pro-{suffix}@test.com",
        "subscriptionType": "PRO",
        "suspended": False,
        "maxConcurrentBookings": 0,
    }
    updated = request("PUT", f"{MEMBER_URL}/{member_id}", expected_statuses={200}, json=updated_payload).json()
    expect(updated["subscriptionType"] == "PRO", "PUT /api/members/{id} doit mettre a jour l'abonnement")
    expect(updated["maxConcurrentBookings"] == 5, "Un membre PRO doit avoir un quota de 5")

    request("PATCH", f"{MEMBER_URL}/{member_id}/suspension", expected_statuses={200}, params={"suspended": "true"})
    expect(get_member(member_id, expected_statuses={200}).json()["suspended"] is True, "PATCH suspension=true a echoue")

    request("PATCH", f"{MEMBER_URL}/{member_id}/suspension", expected_statuses={200}, params={"suspended": "false"})
    expect(get_member(member_id, expected_statuses={200}).json()["suspended"] is False, "PATCH suspension=false a echoue")

    get_member(999999999, expected_statuses={404})


def test_reservation_rules_and_routes():
    suffix = unique_suffix()
    log("\n3. Verification Reservation Service et regles metier...")

    request("GET", RESERVATION_URL, expected_statuses={200})

    room_a = create_room(f"Room A {suffix}")
    room_b = create_room(f"Room B {suffix}", room_type="OPEN_SPACE", hourly_rate=25)
    room_c = create_room(f"Room C {suffix}", room_type="PRIVATE_OFFICE", hourly_rate=60)
    member_quota = create_member(f"Quota {suffix}", f"quota-{suffix}@test.com", "BASIC")
    member_other = create_member(f"Other {suffix}", f"other-{suffix}@test.com", "PRO")

    reservation_1 = create_reservation(
        active_slot_payload(room_a["id"], member_quota["id"]),
        expected_statuses={200, 201},
    ).json()
    expect(reservation_1["status"] == "CONFIRMED", "La reservation 1 doit etre en statut CONFIRMED")

    fetched = get_reservation(reservation_1["id"], expected_statuses={200}).json()
    expect(fetched["roomId"] == room_a["id"], "GET /api/reservations/{id} doit retourner la bonne reservation")

    wait_until(
        "La salle devrait devenir indisponible apres la creation de la reservation",
        lambda: get_room(room_a["id"], expected_statuses={200}).json()["available"] is False,
    )

    overlap_response = create_reservation(
        active_slot_payload(room_a["id"], member_other["id"]),
        expected_statuses={400},
    )
    expect(
        "already booked" in overlap_response.text,
        "La reservation chevauchante doit etre refusee",
    )

    invalid_dates = {
        "roomId": room_b["id"],
        "memberId": member_other["id"],
        "startDateTime": (datetime.now() + timedelta(hours=1)).isoformat(),
        "endDateTime": datetime.now().isoformat(),
    }
    invalid_response = create_reservation(invalid_dates, expected_statuses={400})
    expect("end date must be after start date" in invalid_response.text, "Les dates invalides doivent etre rejetees")

    reservation_2 = create_reservation(
        active_slot_payload(room_b["id"], member_quota["id"]),
        expected_statuses={200, 201},
    ).json()
    expect(reservation_2["status"] == "CONFIRMED", "La reservation 2 doit etre en statut CONFIRMED")

    wait_until(
        "Le membre BASIC devrait etre suspendu apres deux reservations actives",
        lambda: get_member(member_quota["id"], expected_statuses={200}).json()["suspended"] is True,
    )

    third_response = create_reservation(
        active_slot_payload(room_c["id"], member_quota["id"]),
        expected_statuses={400},
    )
    expect("Member is suspended" in third_response.text, "La 3e reservation doit etre refusee pour suspension")

    completed = request("POST", f"{RESERVATION_URL}/{reservation_1['id']}/complete", expected_statuses={200}).json()
    expect(completed["status"] == "COMPLETED", "La reservation 1 doit passer en COMPLETED")

    wait_until(
        "La salle doit redevenir disponible apres completion",
        lambda: get_room(room_a["id"], expected_statuses={200}).json()["available"] is True,
    )

    cancel_completed = request("POST", f"{RESERVATION_URL}/{reservation_1['id']}/cancel", expected_statuses={400})
    expect(cancel_completed.status_code == 400, "Annuler une reservation completee doit echouer")

    cancelled = request("POST", f"{RESERVATION_URL}/{reservation_2['id']}/cancel", expected_statuses={200}).json()
    expect(cancelled["status"] == "CANCELLED", "La reservation 2 doit passer en CANCELLED")

    wait_until(
        "Le membre doit etre desuspendu apres liberation d'une reservation",
        lambda: get_member(member_quota["id"], expected_statuses={200}).json()["suspended"] is False,
    )

    get_reservation(999999999, expected_statuses={404})


def test_kafka_room_deletion():
    suffix = unique_suffix()
    log("\n4. Verification propagation Kafka a la suppression d'une salle...")

    room = create_room(f"Delete room {suffix}")
    member = create_member(f"Delete room member {suffix}", f"delete-room-{suffix}@test.com", "BASIC")
    reservation = create_reservation(
        active_slot_payload(room["id"], member["id"]),
        expected_statuses={200, 201},
    ).json()

    request("DELETE", f"{ROOM_URL}/{room['id']}", expected_statuses={204})

    wait_until(
        "La reservation doit etre annulee apres suppression de la salle",
        lambda: get_reservation(reservation["id"], expected_statuses={200}).json()["status"] == "CANCELLED",
    )


def test_kafka_member_deletion():
    suffix = unique_suffix()
    log("\n5. Verification propagation Kafka a la suppression d'un membre...")

    room = create_room(f"Delete member room {suffix}")
    member = create_member(f"Delete member {suffix}", f"delete-member-{suffix}@test.com", "BASIC")
    reservation = create_reservation(
        active_slot_payload(room["id"], member["id"]),
        expected_statuses={200, 201},
    ).json()

    request("DELETE", f"{MEMBER_URL}/{member['id']}", expected_statuses={204})

    wait_until(
        "La reservation doit etre supprimee apres suppression du membre",
        lambda: get_reservation(reservation["id"], expected_statuses={404}).status_code == 404,
    )


def main():
    log("=== Debut des tests API complets ===")
    wait_for_gateway()
    test_room_routes()
    test_member_routes()
    test_reservation_rules_and_routes()
    test_kafka_room_deletion()
    test_kafka_member_deletion()
    log("\n=== Tous les tests se sont termines avec succes ===")


if __name__ == "__main__":
    try:
        main()
    except TestFailure as exc:
        print(f"\nECHEC: {exc}")
        sys.exit(1)
    except requests.RequestException as exc:
        print(f"\nERREUR RESEAU: {exc}")
        sys.exit(1)
