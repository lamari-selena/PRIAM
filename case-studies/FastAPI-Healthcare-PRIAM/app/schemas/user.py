from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime
from enum import Enum

class UserRole(str, Enum):
    ADMIN = "admin"
    DOCTOR = "doctor"
    PATIENT = "patient"
    STAFF = "staff"

# Shared properties
class UserBase(BaseModel):
    email: EmailStr
    username: str
    is_active: bool = True
    role: UserRole

# Properties to receive on user creation
class UserCreate(UserBase):
    password: str
    reference_id: Optional[int] = None  # ID reference to patient or doctor

# Properties to receive on user update
class UserUpdate(BaseModel):
    email: Optional[EmailStr] = None
    is_active: Optional[bool] = None
    role: Optional[UserRole] = None
    reference_id: Optional[int] = None

# Properties shared by models stored in DB
class UserInDBBase(UserBase):
    id: int
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        orm_mode = True

# Properties to return to client
class User(UserInDBBase):
    pass

# Properties stored in DB
class UserInDB(UserInDBBase):
    hashed_password: str

# Token schemas
class Token(BaseModel):
    access_token: str
    token_type: str

class TokenPayload(BaseModel):
    sub: Optional[int] = None
    role: Optional[str] = None

